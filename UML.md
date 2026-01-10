# Sprint Mate - Data Model

## Genel Mimari

### User Domain

```
+------------------+
|      User        |
|------------------|
| id (UUID, PK)    |
| github_url       |
| name             |
| surname          |
+------------------+
        |
        | 1
        |
        | N
+------------------+
|    UserRole      |
|------------------|
| id (UUID, PK)    |
| user_id (FK)     |
| role_id (FK)     |
+------------------+
        |
        | N
        |
        | 1
+------------------+
|      Role        |
|------------------|
| id (Long, PK)    |
| role_name        |  // FRONTEND, BACKEND
+------------------+
```

### Match Domain

```
+------------------+
|      User        |
|------------------|
| id (UUID, PK)    |
+------------------+
        |
        | 1
        |
        | N
+------------------------+
|   MatchParticipant     |
|------------------------|
| id (UUID, PK)          |
| match_id (FK)          |
| user_id (FK)           |
| participant_role       |  // FRONTEND, BACKEND
+------------------------+
        |
        | N
        |
        | 1
+------------------+
|      Match       |
|------------------|
| id (UUID, PK)    |
| status           |  // CREATED, ACTIVE, COMPLETED
| created_at       |
| expires_at       |
+------------------+
        |
        | 1
        |
        | 1
+--------------------------+
|    MatchCompletion       |
|--------------------------|
| id (UUID, PK)            |
| match_id (FK, UNIQUE)    |
| completed_at             |
| repo_url                 |  // nullable
+--------------------------+
```

### Project Domain

```
+----------------------+
|   ProjectTemplate    |
|----------------------|
| id (UUID, PK)        |
| title                |
| description          |
+----------------------+
        |
        | 1
        |
        | N
+----------------------+
|    MatchProject      |
|----------------------|
| id (UUID, PK)        |
| match_id (FK)        |
| project_template_id  |
| start_date           |
| end_date             |
+----------------------+
        |
        | N
        |
        | 1
+------------------+
|      Match       |
|------------------|
| id (UUID, PK)    |
+------------------+
```

---

## Entity Açıklamaları

### User
Platformdaki geliştiricileri temsil eder, GitHub URL'i ile kimlik doğrulama yapılır.

### Role
Geliştirici rol tiplerini tutan lookup tablosu (FRONTEND, BACKEND).

### UserRole
User ve Role arasındaki many-to-many ilişkiyi sağlayan join table.

### Match
Frontend ve backend geliştiricilerin eşleştirilmesini temsil eder, yaşam döngüsü: CREATED → ACTIVE → COMPLETED.

### MatchParticipant
Bir kullanıcının bir match'e katılımını ve o match'teki rolünü (FRONTEND/BACKEND) tutar.

### MatchCompletion
Tamamlanmış bir match'in kayıtlarını ve opsiyonel olarak proje repo URL'ini saklar.

### ProjectTemplate
Yeniden kullanılabilir proje şablonlarını tanımlar.

### MatchProject
Bir match'e atanmış projeyi ve başlangıç-bitiş tarihlerini tutar.

---

## Enum Tipleri

### MatchStatus
`CREATED` - Match oluşturuldu  
`ACTIVE` - Aktif olarak çalışılıyor  
`COMPLETED` - Tamamlandı

### RoleName
`FRONTEND` - Frontend geliştirici  
`BACKEND` - Backend geliştirici

### ParticipantRole
`FRONTEND` - Match'te frontend görevinde  
`BACKEND` - Match'te backend görevinde

---

## Önemli İlişkiler

- **User ↔ Role**: Many-to-Many (UserRole üzerinden) - Kullanıcı birden fazla role sahip olabilir
- **Match ↔ MatchParticipant**: One-to-Many (Her match'in 2 katılımcısı olmalı: 1 frontend, 1 backend)
- **Match ↔ MatchCompletion**: One-to-One (Her match bir kere tamamlanır)
- **Match ↔ MatchProject**: One-to-Many (Bir match birden fazla proje üzerinde çalışabilir)
- **ProjectTemplate ↔ MatchProject**: One-to-Many (Şablonlar tekrar kullanılabilir)

---

## Teknik Detaylar

- **ID Stratejisi**: UUID (Role hariç - o Long kullanır çünkü küçük lookup table)
- **Timestamp**: `@CreationTimestamp` ile database tarafından otomatik set edilir
- **Enum Storage**: `EnumType.STRING` (güvenli ve okunabilir)
- **Lombok**: `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **Database**: H2 (in-memory), JPA/Hibernate ile code-first yaklaşım

---

**Son Güncelleme:** Phase 2 - Core Data Model  
**Tarih:** Ocak 2026
