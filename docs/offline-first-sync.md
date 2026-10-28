# Offline-first local data and server sync

## Local-first guarantee

RP stores commitment state on-device in Room/SQLite. Saving a commitment does not require network access. The commitment and its outbox event are written in one Room transaction.

## What may sync

Only explicitly queued, low-sensitivity records:
- commitment duration and deadline;
- a small set of intervention outcome events;
- event IDs and timestamps for deduplication.

RP does not queue images, screenshots, messages, page contents, video-call contents, or browser history.

## Connectivity and retries

WorkManager schedules a unique job with a connected-network constraint. Android waits while the device is offline and runs the job when a network becomes available. Temporary network/server errors are retried. Events are marked synced only when the server acknowledges their IDs in acceptedIds.

The outbox is retained if no endpoint is configured. Configure the endpoint at build time:

    gradle -PrpSyncBaseUrl=https://your-api.example.com test assembleDebug

HTTPS is required by the app manifest.

## API contract (v1)

POST /api/v1/sync/batch

Request:
    {
      "schemaVersion": 1,
      "events": [
        {
          "id": "client-generated-uuid",
          "type": "commitment.updated",
          "createdAtEpochMillis": 1790000000000,
          "payload": {
            "selectedDurationMinutes": 15,
            "activeDeadlineEpochMillis": 1790000900000,
            "updatedAtEpochMillis": 1790000000000
          }
        }
      ]
    }

Successful response (HTTP 200):
    { "acceptedIds": ["client-generated-uuid"] }

The server must authenticate the user/device before accepting records, validate event types and sizes, enforce per-user ownership, and make event ID insertion idempotent. It must not trust client timestamps or treat client events as proof that a restriction was enforced.

## Current boundary

The Android client transport and contract are implemented. End-to-end sync is not complete until a real HTTPS endpoint with authentication and durable storage is deployed and configured.
