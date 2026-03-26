#!/bin/bash

# Load environment variables from .env file
export $(cat .env | xargs)

# Run the application
mvn spring-boot:run