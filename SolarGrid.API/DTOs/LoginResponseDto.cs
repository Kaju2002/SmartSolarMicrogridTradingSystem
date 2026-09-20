namespace SolarGrid.API.DTOs;

public class LoginResponseDto
{
    public bool Success { get; set; }

    public string Message { get; set; } = string.Empty;

    public string? UserType { get; set; }

    public string? UserId { get; set; }

    public string? FullName { get; set; }
}