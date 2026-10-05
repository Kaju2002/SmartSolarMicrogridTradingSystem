/*
 * File: UpdateAvailableSlotsDto.cs
 * Description: Free battery slots set by a Grid Operator
 * Author: Gabilan (Station Management)
 * Date: 04/10/2026
 */
namespace SolarGrid.API.DTOs;

public class UpdateAvailableSlotsDto
{
    public int AvailableSlots { get; set; }
    // 0 to the station's BatterySlots
}
