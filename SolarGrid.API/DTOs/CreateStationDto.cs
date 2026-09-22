/*
 * File: CreateStationDto.cs
 * Description: Create station request
 * Author: Gabilan (Station Management)
 * Date: 21/09/2026
 */
namespace SolarGrid.API.DTOs;

public class CreateStationDto
{
    public string StationName { get; set; } = string.Empty;
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public double CapacityKWh { get; set; }
    public int BatterySlots { get; set; }
    public string OpenTime { get; set; } = "06:00";
    public string CloseTime { get; set; } = "18:00";
    public string CreatedBy { get; set; } = string.Empty;
    // Backoffice user id
}
