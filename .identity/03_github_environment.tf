###############################
# Variables of the Repository #
###############################
resource "github_actions_variable" "repo_env" {
  for_each = var.env_short == "p" ? local.repo_env : {}

  repository    = local.github.repository
  variable_name = each.key
  value         = each.value
}

#############################
# Secrets of the Repository #
#############################
resource "github_actions_secret" "repo_secrets" {
  for_each = var.env_short == "p" ? local.repo_secrets : {}

  repository      = local.github.repository
  secret_name     = each.key
  plaintext_value = each.value
}


