/*
 * File: PasswordReset.cs
 * Description: One forgot-password request: hashed e-mail code, then a short-lived reset token
 * Author: Vithusha (Identity and Access)
 * Date: 04/10/2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SolarGrid.API.Models;

public class PasswordReset
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonRepresentation(BsonType.ObjectId)]
    public string UserId { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;
    // Lower-case, so look-ups do not depend on how the user typed it

    public string CodeHash { get; set; } = string.Empty;
    // BCrypt of the 4-digit code; emptied once the code is used

    public DateTime CodeExpiresAt { get; set; }

    public int Attempts { get; set; }
    // Wrong code tries; the code stops working at the limit

    public string? ResetTokenHash { get; set; }
    // SHA-256 of the token handed out after a correct code

    public DateTime? ResetTokenExpiresAt { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
