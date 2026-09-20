namespace SolarGrid.API.DTOs;

public class UpdateUserStatusDto
{
    public string UserId { get; set; } = string.Empty;
    public string NewStatus { get; set; } = string.Empty;
    // "Active" | "Deactivated"
}
