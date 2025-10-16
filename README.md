
# Human-in-loop-system-lyzr-hackathon-submission

A Human-in-the-Loop system enabling agents to pause workflows, request human approvals or feedback, and resume execution asynchronously. It features complete state management, rollback, and resilience against delays or failures, supporting multi-channel inputs and event-driven orchestration for reliable automation.

## Project Demo Video
https://youtu.be/wCUD1J6e5uI

1. Asynchronous System Design

To achieve this, I separated the agent’s workflow into independent asynchronous steps.
Instead of keeping the agent in a wait state, the system:

- Saves the current context (state and metadata) to a persistent store.

- Suspends the flow until the user provides feedback.

- When the feedback arrives, the saved context is fed back into the agent, allowing it to continue from the exact previous point.

2. Resilience and Fault Tolerance

Next, I needed to ensure that the system could:
- Survive restarts or crashes

- Recover state and resume processing

- Handle retries and backoffs gracefully

To achieve this, I leveraged AWS SQS (Simple Queue Service), which:

- Decouples system components for asynchronous communication.

- Guarantees message delivery even after service restarts.

- Enables message retry with exponential backoff for failure scenarios.
```bash
NOTE: i have not added the IAM policies which i used to provision the SQS and S3 and the how i have attached them, if you want to run this, you will need to provision them yourself.
At the bottom will be the fields that need to provisioned by yourslef to run this
```

3. System Architecture
<img width="1959" height="1169" alt="Image" src="https://github.com/user-attachments/assets/18f9f408-c7ec-43fe-92d7-5b1fb3b7abe4" />

```bash
NOTE for the two entities:
AgentRequest: Represents a single logical task initiated by the agent.
ApprovalRound: A subset of AgentRequest that represents a single approval step.
Each AgentRequest can contain multiple ApprovalRounds.
```

4. Workflow Summary

Agent → Backend Communication
The agent sends a request to the backend.
The backend parses and stores it in the database.

Scheduled Processor (every 1–2 minutes)
A scheduled method scans for pending requests and publishes them to the ApprovalRound SQS queue.

ApprovalRound Processor (Async)
This service asynchronously listens to the queue, triggers notifications (via email or Slack), and requeues the message with a backoff delay.
This allows for automated reminder messages over time.

User Feedback Handling
When the user approves, rejects, or adds feedback:

The corresponding ApprovalRound is updated in the database.

The data is sent to the Callback SQS queue.

Callback Processor
A separate scheduled/async method consumes the callback queue and delivers the updated approval data back to the agent, allowing it to continue the next stage of processing.


## Database Architecture
<img width="2166" height="636" alt="Image" src="https://github.com/user-attachments/assets/e5cfead7-1493-4e1c-9c61-82eebeb3662b" />

## frontend
<img width="1089" height="858" alt="Image" src="https://github.com/user-attachments/assets/e36c15f3-4084-4035-8655-36fc773e633f" />

<img width="1101" height="865" alt="Image" src="https://github.com/user-attachments/assets/06201916-eed6-4a8e-b983-caf63f58f3d7" />


## Running Locally
Anyone who forks this repo will need to set the following environment variables (typically in an .env file or system environment):
```bash
DB_USERNAME=
DB_PASSWORD=
MAIL_USERNAME=
MAIL_PASSWORD=
AWS_SQS_REQUEST_QUEUE_URL=
AWS_SQS_DLQ_URL=
AWS_SQS_CALLBACK_QUEUE_URL=
AWS_SQS_CALLBACK_DLQ_URL=
AWS_S3_BUCKET_NAME=
```
