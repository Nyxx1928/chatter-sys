# Security Hardening: WebSocket Authorization, XSS Protection, and CSRF Defense

## Introduction

This bugfix addresses three critical security vulnerabilities in the chat application that could allow attackers to compromise user data and perform unauthorized actions:

1. **Missing WebSocket Authorization**: Any authenticated user can send messages to ANY room without permission checks, allowing unauthorized access to private conversations
2. **XSS (Cross-Site Scripting) Vulnerability**: Messages aren't sanitized, allowing attackers to inject malicious scripts that execute in other users' browsers
3. **CSRF (Cross-Site Request Forgery) Protection**: The application lacks CSRF tokens, allowing attackers to perform actions on behalf of users from malicious websites

These vulnerabilities pose significant security risks and must be fixed before the application handles sensitive user data.

## Bug Analysis

### Current Behavior (Defect)

#### WebSocket Authorization Issues

1.1 WHEN an authenticated user sends a message via WebSocket to a room they are NOT a member of THEN the system broadcasts the message to that room without checking membership authorization

1.2 WHEN an authenticated user sends a message via WebSocket THEN the system only validates that the user is a member at the service layer, but the WebSocket controller does not enforce per-message authorization checks

1.3 WHEN a user attempts to join a room via WebSocket THEN the system verifies membership but does not prevent unauthorized room access attempts from being logged or monitored

#### XSS Vulnerabilities

1.4 WHEN a user sends a message containing HTML/JavaScript code (e.g., `<script>alert('xss')</script>`) THEN the system stores and broadcasts the raw content without sanitization

1.5 WHEN the frontend displays a message THEN the message content is rendered as plain text in a `<p>` tag without HTML escaping, allowing script injection if content contains HTML entities

1.6 WHEN a user sends a message with event handlers (e.g., `<img src=x onerror="alert('xss')">`) THEN the system does not sanitize or validate the content, allowing the payload to execute in other users' browsers

#### CSRF Protection Issues

1.7 WHEN a user performs state-changing operations (POST, PUT, DELETE) via REST API THEN the system does not require or validate CSRF tokens, allowing attackers to forge requests from malicious websites

1.8 WHEN a user is logged in and visits a malicious website THEN an attacker can craft requests to create rooms, send messages, or modify user data on behalf of the victim without their knowledge

### Expected Behavior (Correct)

#### WebSocket Authorization

2.1 WHEN an authenticated user sends a message via WebSocket to a room THEN the system SHALL verify the user is a member of that room before broadcasting, and reject unauthorized attempts with an error

2.2 WHEN an authenticated user attempts to send a message to a room they are not a member of THEN the system SHALL throw an UnauthorizedException and send an error message to the user's error queue

2.3 WHEN a user attempts to join a room via WebSocket THEN the system SHALL verify membership exists and log all join attempts (successful and failed) for security auditing

2.4 WHEN a user sends a message THEN the system SHALL validate authorization at both the controller and service layer to prevent bypassing security checks

#### XSS Protection

2.5 WHEN a user sends a message containing HTML/JavaScript code THEN the system SHALL sanitize the content by escaping HTML entities before storing in the database

2.6 WHEN the frontend displays a message THEN the system SHALL render the content as plain text with proper HTML escaping to prevent script execution

2.7 WHEN a user sends a message with event handlers or script tags THEN the system SHALL strip or escape all dangerous HTML/JavaScript patterns before persistence and display

2.8 WHEN a message is retrieved from the database THEN the system SHALL ensure the content is safe for display without additional sanitization needed on the frontend

#### CSRF Protection

2.9 WHEN a user performs a state-changing operation (POST, PUT, DELETE) via REST API THEN the system SHALL require a valid CSRF token in the request headers

2.10 WHEN a user submits a form or makes an API request THEN the system SHALL validate the CSRF token matches the user's session token before processing the request

2.11 WHEN an attacker attempts to forge a request from a malicious website THEN the system SHALL reject the request due to missing or invalid CSRF token

2.12 WHEN a user logs in THEN the system SHALL generate and provide a CSRF token that must be included in all subsequent state-changing requests

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a user who IS a member of a room sends a message THEN the system SHALL CONTINUE TO broadcast the message to all room subscribers without additional delays

3.2 WHEN a user sends a message with legitimate content (no HTML/scripts) THEN the system SHALL CONTINUE TO store and display the message exactly as sent

3.3 WHEN a user performs authorized operations with valid CSRF tokens THEN the system SHALL CONTINUE TO process the requests normally without additional friction

3.4 WHEN a user joins a room they are already a member of THEN the system SHALL CONTINUE TO broadcast the JOIN system message to other room members

3.5 WHEN a user sends a message with special characters (emoji, unicode, etc.) THEN the system SHALL CONTINUE TO preserve and display these characters correctly

3.6 WHEN a user retrieves message history THEN the system SHALL CONTINUE TO return messages in chronological order with correct timestamps and sender information
