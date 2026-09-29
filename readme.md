# 🎬 Netflix-Style Video Streaming Platform

A scalable **video streaming platform** built using **Spring Boot Microservices, Apache Kafka, Redis, MariaDB, AWS S3, FFmpeg, and HLS**.

The project demonstrates how a modern video streaming platform can handle video uploads, asynchronous processing, multi-quality encoding, cloud storage, event-driven communication, caching, and secure HLS streaming.

---

## 📑 Table of Contents

- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Microservices](#-microservices)
- [Video Processing Flow](#-video-processing-flow)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Environment Variables](#-environment-variables)
- [Running the Project](#-running-the-project)
- [API Endpoints](#-api-endpoints)
- [Kafka Events](#-kafka-events)
- [HLS Streaming](#-hls-streaming)
- [Redis](#-redis)
- [AWS S3](#-aws-s3)
- [FFmpeg](#-ffmpeg)
- [Complete End-to-End Flow](#-complete-end-to-end-flow)
- [Docker Services](#-docker-services)
- [Troubleshooting](#-troubleshooting)
- [Future Improvements](#-future-improvements)
- [Author](#-author)

---

# 🏗️ Architecture

```mermaid
flowchart LR

    Client["🌐 Client / Browser"]

    Content["🎬 Content Service<br/>:8080"]
    Video["📤 Video Service<br/>:8082"]
    Encoding["⚙️ Encoding Service<br/>:8083"]
    Streaming["📺 Streaming Service<br/>:8084"]

    Kafka["🔄 Apache Kafka"]
    Redis["⚡ Redis"]
    S3["☁️ AWS S3"]
    FFmpeg["🎞️ FFmpeg"]
    DB["🗄️ MariaDB"]

    Client --> Content
    Client --> Video
    Client --> Streaming

    Content --> DB

    Video --> S3
    Video --> Kafka

    Kafka --> Encoding
    Encoding --> S3
    Encoding --> FFmpeg
    Encoding --> Kafka

    Kafka --> Content
    Kafka --> Streaming

    Streaming --> Redis
    Streaming --> S3