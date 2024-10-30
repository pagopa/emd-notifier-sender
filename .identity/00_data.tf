data "azurerm_key_vault" "key_vault_core" {
  count               = var.env_short == "p" ? 1 : 0
  name                = "${var.prefix}-${var.env_short}-kv"
  resource_group_name = "${var.prefix}-${var.env_short}-sec-rg"
}

# Github
data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

# Key Vault - Sonar Token
data "azurerm_key_vault_secret" "sonar_token" {
  count = var.env_short == "p" ? 1 : 0

  key_vault_id = data.azurerm_key_vault.key_vault_core[0].id
  name         = "sonar-cloud"
}


