# API Contract v1

This document defines the first stable contract between the iOS client and the backend skeleton.
The current implementation keeps the URL, method, core DTOs, and response envelope stable.

## Base rules
- Base path: `/api`
- Content type: `application/json`
- Authentication: bearer token in `Authorization: Bearer <token>`
- Idempotent create endpoints may send `Idempotency-Key`
- Public endpoints remain open for bootstrap, auth, template browsing, upload policy, and generation create/detail.
- Protected endpoints now require a valid access token. Missing or invalid bearer token returns `401`; authenticated but disallowed access returns `403`.
- Email login and Apple login now create or reuse a persisted user record on the server side.
- Generation tasks are now persisted. Authenticated requests are attached to the current user; anonymous requests remain queryable by task ID but are not included in protected history APIs.
- Internal task orchestration now uses a dedicated status-update endpoint protected by `X-Internal-Token`.
- Authenticated generation requests now reserve template credits at creation time. `SUCCESS` confirms the deduction; `FAILED` and `REFUNDED` release the reserved amount back to the user account.
- Mock provider dispatch and webhook skeleton are now available for end-to-end integration testing.
- Media handling now uses managed object keys. Public APIs return resolved URLs while internal/provider flows may pass storage object keys.
- Source uploads, generated assets, and provider callbacks are now persisted as metadata/audit records for traceability.
- IAP verification now requires authentication, persists orders, grants credits, and treats `transactionId` as an idempotency key.
- Credit mutations are now persisted to a unified ledger for IAP grants and generation reserve/consume/release/refund flows.

## Response envelope

Success:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "traceId": "3ab2f1f4f9b54c42a2c4f84c1c0f7b41",
  "timestamp": "2026-03-31T04:00:00Z"
}
```

Error:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": {
      "email": "must be a well-formed email address"
    }
  },
  "traceId": "3ab2f1f4f9b54c42a2c4f84c1c0f7b41",
  "timestamp": "2026-03-31T04:00:00Z"
}
```

## Error codes
- `VALIDATION_ERROR`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `INSUFFICIENT_CREDITS`
- `NOT_FOUND`
- `CONFLICT`
- `INTERNAL_ERROR`

## Protected endpoints
- `GET /api/history`
- `DELETE /api/history/{id}`
- `GET /api/credits/balance`
- `GET /api/credits/ledger`
- `POST /api/account/delete`
- `POST /api/iap/verify`

## Domain enums
- `GenerationStatus`: `CREATED`, `AUDITING`, `READY_TO_DISPATCH`, `RUNNING`, `POST_PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`

## Endpoints

### POST `/api/auth/apple/login`
Request fields:
- `identityToken`: string, required
- `authorizationCode`: string, optional

Response fields:
- `userId`
- `accessToken`
- `refreshToken`
- `expiresIn`
- `isNewUser`
- server will create a new user row on first successful login

### POST `/api/auth/email/send-code`
Request fields:
- `email`: string, required
- `scene`: string, required

Response fields:
- `cooldownSeconds`
- `maskedDestination`

### POST `/api/auth/email/login`
Request fields:
- `email`: string, required
- `code`: string, required

Response fields:
- `userId`
- `accessToken`
- `refreshToken`
- `expiresIn`
- `isNewUser`
- server will create a new user row on first successful login

### POST `/api/account/delete`
Authentication:
- access token required

Request fields:
- `reason`: string, optional
- `confirmText`: string, optional

Response fields:
- `status`
- `scheduledDeletionAt`
- `cleanupJobId`

Behavior:
- server creates or refreshes a persisted cleanup job
- user-owned generation tasks and media assets are marked `DELETION_SCHEDULED`
- internal cleanup execution will later move them to terminal lifecycle states

### GET `/api/config/bootstrap`
Response fields:
- `productName`
- `iosReviewMode`
- `iapEnabled`
- `supportedLoginMethods`
- `legalDocuments`
- `generation`

### GET `/api/templates`
Response fields:
- array of template summary items:
  - `id`
  - `name`
  - `styleCode`
  - `previewUrl`
  - `priceCredits`
  - `enabled`

### GET `/api/templates/{id}`
Response fields:
- `id`
- `name`
- `styleCode`
- `description`
- `previewUrl`
- `sampleImages`
- `priceCredits`
- `enabled`
- `supportedAspectRatios`

### POST `/api/upload/policy`
Request fields:
- `fileName`: string, required
- `contentType`: string, required

Response fields:
- `objectKey`
- `uploadUrl`
- `method`
- `headers`
- `expiresInSeconds`

Behavior:
- validates `contentType` against the configured allow-list
- returns a managed source object key under the upload prefix

### POST `/api/generations`
Headers:
- `Idempotency-Key`: optional in current skeleton, recommended in client implementation

Request fields:
- `templateId`: string, required
- `inputObjectKey`: string, required
- `count`: integer, required

Request constraints:
- `inputObjectKey` must come from the managed upload policy and stay within the configured source prefix

Response fields:
- `taskId`
- `status`
- `pollAfterSeconds`
- authenticated requests persist the task under the current user when a bearer token is present
- authenticated requests reserve `template.priceCredits` from the user balance; insufficient balance returns `INSUFFICIENT_CREDITS`

### GET `/api/generations/{taskId}`
Response fields:
- `taskId`
- `status`
- `progressPercent`
- `previewUrls`
- `resultUrls`
- `failedReason`

### POST `/api/internal/generations/{taskId}/status`
Authentication:
- internal header `X-Internal-Token` required

Request fields:
- `status`: one of `AUDITING`, `READY_TO_DISPATCH`, `RUNNING`, `POST_PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`
- `progressPercent`: optional
- `providerTaskId`: required on first `RUNNING` update
- `previewUrls`: optional
- `resultUrls`: required for `SUCCESS`
- `failedReason`: required for `FAILED`

Behavior:
- validates allowed state transitions
- updates persisted task state and progress
- intended for future dispatcher/webhook integration

### POST `/api/internal/generations/{taskId}/dispatch`
Authentication:
- internal header `X-Internal-Token` required

Behavior:
- requires task status `READY_TO_DISPATCH`
- dispatches the task through the configured provider adapter
- writes `providerTaskId`
- advances the task to `RUNNING`

### POST `/api/providers/mock/webhook`
Authentication:
- provider header `X-Provider-Token` required

Request fields:
- `providerTaskId`: string, required
- `status`: one of `RUNNING`, `POST_PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`
- `progressPercent`: optional
- `previewUrls`: optional
- `resultUrls`: required for `SUCCESS`
- `failedReason`: required for `FAILED`

Behavior:
- resolves the persisted task by `providerTaskId`
- reuses the same internal state machine rules as the internal status endpoint
- `previewUrls` / `resultUrls` may contain managed object keys; public task detail responses always return resolved URLs

### POST `/api/internal/account-cleanup/{cleanupJobId}/execute`
Authentication:
- internal header `X-Internal-Token` required

Behavior:
- executes the persisted cleanup plan for the target user
- marks owned generation tasks as `PURGED`
- marks owned media assets as `DELETED`
- updates the user status to `DELETED`

### GET `/api/history`
Authentication:
- access token required

Response fields:
- array of history items:
  - `taskId`
  - `templateName`
  - `status`
  - `coverUrl`
  - `createdAt`
- only includes non-deleted tasks owned by the current authenticated user

### DELETE `/api/history/{id}`
Authentication:
- access token required

Response fields:
- `deleted`
- `historyId`
- deletes are implemented as a soft-delete flag on the persisted task

### POST `/api/iap/verify`
Authentication:
- access token required

Request fields:
- `productId`: string, required
- `transactionId`: string, required
- `receiptData`: string, required

Response fields:
- `orderId`
- `status`
- `creditsGranted`
- `balanceAfter`

Behavior:
- persists a verified IAP order per unique `transactionId`
- repeated verification of the same `transactionId` for the same user returns the existing order without granting credits twice

### GET `/api/credits/balance`
Authentication:
- access token required

Response fields:
- `availableCredits`
- `frozenCredits`
- `currency`
- current skeleton reads these values from persisted user account fields
- `frozenCredits` reflects reserved generation spend not yet settled
- all balance-changing operations are also written to the internal credit ledger

### GET `/api/credits/ledger`
Authentication:
- access token required

Response fields:
- `entries`
- `total`

Entry fields:
- `entryId`
- `entryType`
- `availableDelta`
- `frozenDelta`
- `balanceAfterAvailable`
- `balanceAfterFrozen`
- `generationTaskId`
- `iapOrderId`
- `description`
- `createdAt`

Behavior:
- returns the current authenticated user's credit ledger entries in reverse chronological order

### GET `/api/internal/admin/credits/ledger`
Authentication:
- internal header `X-Internal-Token` required

Query parameters:
- `userId`: optional
- `generationTaskId`: optional
- `iapOrderId`: optional

Behavior:
- requires at least one filter
- returns matching ledger entries for internal support and admin use
