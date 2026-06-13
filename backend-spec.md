# Openia Backend Architecture & Compatibility Specification

This specification outlines the production-ready schemas, database structures, authentication protocols, and real-time synchronizations to securely bridge the Android application with a TypeScript / Node.js or NestJS backend.

---

## 1. Database Schema (PostgreSQL)

The layout of SQL tables is modeled directly after the Android Room Entities in `com.example.data.model.PostModels` to enable clean offline-first syncing and immediate object mapping.

```sql
-- 1. User Profiles
CREATE TABLE user_profiles (
    username VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(150) NOT NULL,
    bio TEXT DEFAULT '',
    avatar_seed VARCHAR(1) NOT NULL DEFAULT 'Y',
    base_followers INT NOT NULL DEFAULT 12,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Feed Posts
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    post_type VARCHAR(10) NOT NULL, -- 'OPINION' or 'PROBLEM'
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    tags VARCHAR(255) NOT NULL, -- Comma-separated tags
    author VARCHAR(100) NOT NULL REFERENCES user_profiles(username) ON DELETE CASCADE,
    avatar_seed VARCHAR(1) NOT NULL,
    agree_count INT NOT NULL DEFAULT 0,
    disagree_count INT NOT NULL DEFAULT 0,
    upvotes_count INT NOT NULL DEFAULT 0,
    downvotes_count INT NOT NULL DEFAULT 0,
    empathy_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    ai_summary TEXT,
    ai_solutions TEXT,
    ai_consensus TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Comments & Solutions
CREATE TABLE comments (
    id SERIAL PRIMARY KEY,
    post_id INT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author VARCHAR(100) NOT NULL REFERENCES user_profiles(username) ON DELETE CASCADE,
    avatar_seed VARCHAR(1) NOT NULL,
    content TEXT NOT NULL,
    is_solution BOOLEAN NOT NULL DEFAULT FALSE,
    vote_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. User Reactions Tracker
CREATE TABLE user_reactions (
    post_id INT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    reaction_type VARCHAR(15) NOT NULL, -- 'AGREE', 'DISAGREE', 'UPVOTE', 'DOWNVOTE', 'EMPATHY'
    username VARCHAR(100) NOT NULL REFERENCES user_profiles(username) ON DELETE CASCADE,
    PRIMARY KEY (post_id, reaction_type, username)
);

-- 5. Followers Tracker
CREATE TABLE follows (
    follower_username VARCHAR(100) NOT NULL REFERENCES user_profiles(username) ON DELETE CASCADE,
    followed_username VARCHAR(100) NOT NULL REFERENCES user_profiles(username) ON DELETE CASCADE,
    PRIMARY KEY (follower_username, followed_username)
);

-- Indices for rapid queries, feed loading, and filter performance
CREATE INDEX idx_posts_type ON posts(post_type);
CREATE INDEX idx_posts_category ON posts(category);
CREATE INDEX idx_posts_trending ON posts((upvotes_count + agree_count + comment_count + empathy_count) DESC);
CREATE INDEX idx_comments_post_id ON comments(post_id);
```

---

## 2. API Schema Definitions (TypeScript & Zod)

To guarantee type-safe contracts between the frontend and the TypeScript server, the following schemas are declared via **Zod**.

```typescript
import { z } from 'zod';

// JWT Claim Payload Schema
export const TokenPayloadSchema = z.object({
  username: z.string().min(1),
  displayName: z.string(),
  avatarSeed: z.string().length(1),
  role: z.enum(['USER', 'MODERATOR', 'ADMIN']),
  iat: z.number(),
  exp: z.number(),
});
export type TokenPayload = z.infer<typeof TokenPayloadSchema>;

// Post Validation Schema
export const PostSchema = z.object({
  id: z.number().optional(),
  postType: z.enum(['OPINION', 'PROBLEM']),
  title: z.string().min(5, "Title must be at least 5 characters long").max(100),
  content: z.string().min(10, "Detail content must be at least 10 characters"),
  category: z.string().min(2),
  tags: z.string().regex(/^[a-zA-Z0-9,\s]*$/, "Tags must contain alphabetical/comma characters only"),
  author: z.string(),
  avatarSeed: z.string().length(1),
  agreeCount: z.number().default(0),
  disagreeCount: z.number().default(0),
  upvotesCount: z.number().default(0),
  downvotesCount: z.number().default(0),
  empathyCount: z.number().default(0),
  commentCount: z.number().default(0),
  aiSummary: z.string().nullable().optional(),
  aiSolutions: z.string().nullable().optional(),
  aiConsensus: z.string().nullable().optional(),
  timestamp: z.number().default(() => Date.now()),
});
export type PostModel = z.infer<typeof PostSchema>;

// Comment Validation Schema
export const CommentSchema = z.object({
  id: z.number().optional(),
  postId: z.number(),
  author: z.string(),
  avatarSeed: z.string().length(1),
  content: z.string().min(3),
  timestamp: z.number().default(() => Date.now()),
  isSolution: z.boolean().default(false),
  voteCount: z.number().default(0),
});
export type CommentModel = z.infer<typeof CommentSchema>;

// User Profile Validation Schema
export const UserProfileSchema = z.object({
  username: z.string(),
  displayName: z.string().min(2).max(50),
  bio: z.string().max(180).default(""),
  avatarSeed: z.string().length(1).default("Y"),
  baseFollowers: z.number().default(12),
});
export type UserProfileModel = z.infer<typeof UserProfileSchema>;
```

---

## 3. Authentication & Security (OAuth2 & JWT Auth)

Openia's client interfaces have been refactored to consume stateless token-based authorization.

### Auth Workflow Diagram

```
[Android App] ---(1. Exchange OAuth ID Token / Code)---> [NestJS/TypeScript Auth API]
      ^                                                            |
      |                                                  (2. Validate with Google/Local)
      |                                                            |
      |                                                  (3. Upsert Profile & Issue JWT)
      +--------------(4. Access/Refresh Token)---------------------+
```

### JWT Headers
OAuth2 success outcomes issue standard compact JWS values headers:
```http
Authorization: Bearer <JWT_TOKEN>
```

---

## 4. Real-time Synchronization (WebSockets)

Openia uses standard WebSocket event channels for instant interaction updates, consensus updates, and global metrics syncing.

### Client Bound Events

```typescript
export interface ClientToServerEvents {
  subscribeToPost: (postId: number) => void;
  unsubscribeFromPost: (postId: number) => void;
  triggerReaction: (postId: number, type: 'AGREE' | 'DISAGREE' | 'UPVOTE' | 'DOWNVOTE' | 'EMPATHY') => void;
  typingStart: (postId: number) => void;
  typingStop: (postId: number) => void;
}
```

### Server Bound Events

```typescript
export interface ServerToClientEvents {
  postUpdated: (payload: { postId: number; stats: Record<string, number> }) => void;
  commentAdded: (payload: { postId: number; comment: any }) => void;
  reputationChanged: (payload: { username: string; points: number, rankLevel: number }) => void;
  aiSynthesisReady: (payload: { postId: number; summary: string; solutions: string; consensus: string }) => void;
  typingUpdate: (payload: { postId: number; usersTyping: string[] }) => void;
  followerUpdate: (payload: { username: string; followersCount: number }) => void;
}
```

## 5. Caching & Message Brokering (Redis)

Redis is deployed to manage transient states like live view counts, active typers, and to broker events between horizontally scaled Node.js WebSocket instances.

```redis
# 1. Live Typing Indicators (Expiring sets)
SADD typing:post:123 "NavalS"
EXPIRE typing:post:123 10

# 2. Hot Post Caching (TTL 5 mins)
SETEX posts:hot 300 "{...json_array...}"

# 3. Rate Limiting (Token Bucket)
INCR rate:user:NavalS:reactions
EXPIRE rate:user:NavalS:reactions 60
```

## 6. Automated Background Processing (Cron Jobs)

To manage trending matrices and reputation balances over time, the Node.js server registers the following persistent tasks:

### 1. Daily Trending Score Recalculation (Cron: `0 0 * * *`)
Re-align scores using gravity-decay formulas similar to Reddit and Hacker News.
```
Score = (Agrees + Upvotes * 2 + Empathy * 5 + Comments) / (TimeDeltaHours + 2)^1.5
```

### 2. Spam & Toxicity Moderation Flush (Cron: `*/5 * * * *`)
Every 5 minutes, query posts reported/flagged with low sentiment ratings to auto-quarantine.

### 3. Idle User Baseline Follower Growth (Cron: `0 12 * * *`)
Simulates baseline viral growth metrics: active thinkers receive organic follower increments based on positive overall platform reputation scores.
