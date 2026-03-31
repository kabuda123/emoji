# API Contract v1

This document defines the first stable contract between the iOS client and the backend skeleton.
The current implementation keeps the URL, method, core DTOs, and response envelope stable.

## Base rules
- Base path: `/api`
- Content type: `application/json`
- Authentication: bearer token in `Authorization: Bearer <token>`
- Idempotent create endpoints may send `Idempotency-Key`
- Public endpoints remain open for bootstrap, auth, template browsing, upload policy, generation create/detail, and IAP verify.
- Protected endpoints now require a valid access token. Missing or invalid bearer token returns `401`; authenticated but disallowed access returns `403`.
- Email login and Apple login now create or reuse a persisted user record on the server side.
- Generation tasks are now persisted. Authenticated requests are attached to the current user; anonymous requests remain queryable by task ID but are not included in protected history APIs.
- Internal task orchestration now uses a dedicated status-update endpoint protected by `X-Internal-Token`.

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
- `NOT_FOUND`
- `CONFLICT`
- `INTERNAL_ERROR`

## Protected endpoints
- `GET /api/history`
- `DELETE /api/history/{id}`
- `GET /api/credits/balance`
- `POST /api/account/delete`

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

### POST `/api/generations`
Headers:
- `Idempotency-Key`: optional in current skeleton, recommended in client implementation

Request fields:
- `templateId`: string, required
- `inputObjectKey`: string, required
- `count`: integer, required

Response fields:
- `taskId`
- `status`
- `pollAfterSeconds`
- authenticated requests persist the task under the current user when a bearer token is present

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
Request fields:
- `productId`: string, required
- `transactionId`: string, required
- `receiptData`: string, required

Response fields:
- `orderId`
- `status`
- `creditsGranted`
- `balanceAfter`

### GET `/api/credits/balance`
Authentication:
- access token required

Response fields:
- `availableCredits`
- `frozenCredits`
- `currency`
- current skeleton reads these values from persisted user account fields
