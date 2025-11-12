# emd-notifier-sender
Service that manages the forwarding of messages to citizens who have activated the courtesy message service via a PSP App

## Components

---

#### [MessageCoreConsumerService](src/main/java/it/gov/pagopa/notifier/service/MessageCoreConsumerServiceImpl.java)
**(emd-courtesy-message-consumer)**

Consumer component for processing incoming messages from Kafka.

- **Operational flow:**

1. **Message reception**: Receives the message from the `emd-courtesy-message` Kafka queue
2. **Retry extraction**: Retrieves the retry count from the message header
3. **Processing delegation**: Invokes the `processMessage` method of [`MessageService`](src/main/java/it/gov/pagopa/notifier/service/MessageService.java) to start the message delivery process

---

#### [MessageService](src/main/java/it/gov/pagopa/notifier/service/MessageService.java)

Core service responsible for message processing and distribution to TPPs.

- **Operational flow:**

1. **Message processing**: Receives the message from [`MessageCoreConsumerService`](src/main/java/it/gov/pagopa/notifier/service/MessageCoreConsumerServiceImpl.java) and executes `processMessage`
2. **Consent retrieval**: Fetches active consents for the citizen using the `recipientId`
3. **TPP filtering**: Filters the list of citizen's TPPs to include only those that are active by calling `getTppsEnabled` of [`TppConnector`](src/main/java/it/gov/pagopa/notifier/connector/tpp/TppConnector.java)
4. **Message mapping**: For each TPP, the message is mapped by adding:
    - The `idPsp` of the TPP
    - The `entityId` of the TPP
    - A fixed note from configuration parameters
5. **Notification dispatch**: Sends the message to each active TPP via `sendNotifications` method:
    - Saves the message in the repository with `IN_PROGRESS` status and adds it to the Tuple
    - If save fails, adds it with id "REFUSED"
6. **Notification delivery**: If id is not "REFUSED", executes the `sendNotify` method of [`NotifyService`](src/main/java/it/gov/pagopa/notifier/service/NotifyService.java)
7. **Error handling**: On failure, re-queues the message to `emd-courtesy-message` with incremented retry count

---

#### [MessageCoreProducerService](src/main/java/it/gov/pagopa/notifier/service/MessageCoreProducerServiceImpl.java)
**(emd-courtesy-message-producer)**

Producer component for retry management of failed message processing.

- **Operational flow:**

1. **Retry trigger**: Invokes `enqueueMessage` when message processing fails in `MessageService`
2. **Retry limit check**: Verifies if maximum retry count has been reached and returns `Mono.empty()` if so
3. **Re-scheduling**: Otherwise, schedules the message to be re-queued for consumption by [`MessageCoreConsumerService`](src/main/java/it/gov/pagopa/notifier/service/MessageCoreConsumerServiceImpl.java)

---

#### [NotifyService](src/main/java/it/gov/pagopa/notifier/service/NotifyService.java)

Service responsible for notification delivery to TPPs and message deletion operations.

- **Scheduled cleanup flow (`scheduleDeletionTask`):**

1. **Automatic execution**: Runs periodically based on `delete.batchExecutionCron` configuration
2. **Retention calculation**: Computes deletion date as current date minus `retentionPeriodDays`
3. **Cleanup invocation**: Calls `cleanupOldMessages` which internally invokes `deleteMessages`
4. **Logging**: Records deleted count, remaining count, and execution time

- **Message deletion flow (`deleteMessages`):**

1. **Request processing**: Receives [`DeleteRequestDTO`](src/main/java/it/gov/pagopa/notifier/dto/DeleteRequestDTO.java) containing batch size and interval between deletions
2. **Message filtering**: Searches messages using `startDate` and `endDate` filters if provided, otherwise performs `findAll()`
3. **Batch deletion**: Deletes messages in batches with delays between batches to avoid database overload
4. **Result reporting**: Returns number of deleted messages, remaining messages, and execution time

- **TPP notification flow (`sendNotify`):**

1. **Authentication**: POST to TPP's `authenticationUrl` with `contentType` and body parameters from `tokenSection` (configured during TPP onboarding) to obtain `TokenDTO`
2. **Delivery**: POST to TPP's notification URL with:
    - Authorization header containing the token
    - Body: (message + notes) mapped to `BaseMessage`
3. **Status update**: Sets message status to `SENT`
4. **Error handling**: On failure, re-queues the message to `emd-notify-error` Kafka queue with incremented retry count

---

#### [NotifyErrorProducerService](src/main/java/it/gov/pagopa/notifier/service/NotifyErrorProducerService.java)
**(emd-notify-error-producer)**

Producer component for managing failed TPP notification deliveries.

- **Operational flow:**

1. **Failure detection**: Triggered when TPP notification delivery fails in `sendNotify` method
2. **Error queuing**: Invokes `enqueueNotify` method with the message, failed TPP, and retry count
3. **Re-scheduling**: Queues the retry attempt to `emd-notify-error` for consumption by [`NotifyErrorConsumerService`](src/main/java/it/gov/pagopa/notifier/service/NotifyErrorConsumerServiceImpl.java)

---

#### [NotifyErrorConsumerService](src/main/java/it/gov/pagopa/notifier/service/NotifyErrorConsumerServiceImpl.java)
**(emd-notify-error-consumer)**

Consumer component for processing failed TPP notification deliveries.

- **Operational flow:**

1. **Error consumption**: Processes messages from the `emd-notify-error` queue
2. **Payload extraction**: Retrieves [`TppDTO`](src/main/java/it/gov/pagopa/notifier/dto/TppDTO.java) and notification from the payload
3. **Re-delivery attempt**: Invokes `sendNotify` method of [`NotifyService`](src/main/java/it/gov/pagopa/notifier/service/NotifyService.java) to attempt re-delivery to the TPP

---

## Integration Points

- **Azure Event Hubs**:
    - `emd-courtesy-message`: Main message processing queue
    - `emd-notify-error`: Failed send notification to TPP retry queue

## Key Components

- **Kafka Queues**: Used for asynchronous message processing and retry management
- **TPP Integration**: External Third Party Providers for notification delivery
## API Documentation

API specification: [openapi.emd_sender.yml](https://github.com/pagopa/cstar-infrastructure/blob/main/src/domains/mil-app-poc/api/emd_message_sender/openapi.emd_sender.yml)
