# Firefly III Telegram Agent

This project is a Telegram bot that acts as a personal finance AI assistant, helping users manage their [Firefly III](https://firefly-iii.org/) finances through natural language chat. It leverages AI models (via OpenAI or compatible APIs) and integrates with Firefly III using the MCP (Multi-Channel Platform) toolset.

## Project Structure

- `src/main/java/com/qvanphong/fireflyagent/` - Main Java source code for the bot, chat client, and configuration.
- `src/main/resources/application.yml` - Spring Boot configuration (including AI and Telegram settings).
- `Dockerfile` & `Dockerfile-mcp` - Docker setup for both the Telegram bot and the MCP server.
- `build.gradle` - Gradle build configuration.

## Getting Started

### Prerequisites

- Java 17+
- Docker (optional, for containerized deployment)
- Telegram account and bot token
- Firefly III instance and MCP server

### Configuration

Set the following environment variables (or update `application.yml`):

- `TELEGRAM_BOT_USERNAME` - your Telegram bot username (register through BotFather)
- `TELEGRAM_BOT_TOKEN` - Your Telegram bot token
- `TELEGRAM_BOT_OWNER_ID` - Your Telegram id, to make sure bot serves for you only. (you can find through @userinfobot)
- `OPENAI_URL` - OpenAI API compatible URL (Highly recommend to use OpenRouter to explore the best model for your use case)
- `OPENAI_API_KEY` - OpenAI API compatible key
- `FIREFLY_MCP_URL` - URL to Firefly MCP

### Build & Run

#### Locally

```sh
./gradlew clean build
java -jar build/libs/fireflyiii-agent-telegram.jar
```

#### Docker compose
Run `build_docker.sh` first to build source into Docker images.
Change necessaries environment variables in `docker-compose.yml`, please refer (here for Firefly III MCP server env)[https://github.com/etnperlong/firefly-iii-mcp/tree/cc48511591e14a633942b6cc22f407ada5faa5be].
When you are ready, start the docker compose

```sh
docker compose up
```