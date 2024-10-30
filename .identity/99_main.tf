terraform {
  required_version = ">= 1.5"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.116"
    }
    github = {
      source  = "integrations/github"
      version = "~> 6.3"
    }
  }

  backend "azurerm" {}
}

provider "azurerm" {
  features {}
}

provider "github" {
  owner = "pagopa"
}
