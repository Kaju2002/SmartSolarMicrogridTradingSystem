/*
 * File: SolarStationInfo.cs
 * Description: Solar station / hub model
 * Author: Gabilan (Station Management)
 * Date: 21/09/2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SolarGrid.API.Models;

public class SolarStationInfo
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string StationName { get; set; } = string.Empty;

    public double Latitude { get; set; }

    public double Longitude { get; set; }

    public double CapacityKWh { get; set; }

    public int BatterySlots { get; set; }
    // total slots

    public int AvailableSlots { get; set; }
    // free slots

    public string OpenTime { get; set; } = "06:00";
    // operating hours start

    public string CloseTime { get; set; } = "18:00";
    // operating hours end

    public string Status { get; set; } = "Active";
    // Active | Deactivated

    public string? CreatedBy { get; set; }
    // Backoffice user id

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAt { get; set; }
}
