using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SolarGrid.API.Models;

public class User
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string UserType { get; set; } = string.Empty;
    // "Backoffice" | "GridOperator" | "Prosumer"

    public string? Nic { get; set; }
    // Prosumer only

    public string? Username { get; set; }
    // Backoffice / GridOperator only

    public string PasswordHash { get; set; } = string.Empty;

    public string FullName { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public string PhoneNumber { get; set; } = string.Empty;

    public string Status { get; set; } = "PendingApproval";
    // "Active" | "Deactivated" | "PendingApproval"

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }
}