/*
 * File: UpdateStationDto.cs
 * Description: Partial station update
 * Author: Gabilan (Station Management)
 * Date: 21/09/2026
 */
namespace SolarGrid.API.DTOs;

public class UpdateStationDto
{
    public double? CapacityKWh { get; set; }
    public int? BatterySlots { get; set; }
    public string? OpenTime { get; set; }
    public string? CloseTime { get; set; }
    public string? AssignedOperatorId { get; set; }
    // GridOperator user id; empty string clears assignment
    public bool UpdateAssignedOperator { get; set; }
    // when true, AssignedOperatorId is applied (including clear)
}
