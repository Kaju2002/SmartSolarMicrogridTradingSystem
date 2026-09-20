namespace SolarGrid.API.DTOs;

public class LoginRequestDto
{
    public string Identifier { get; set; } = string.Empty;
    // NIC (Prosumer) or Username (Backoffice / GridOperator)

    public string Password { get; set; } = string.Empty;
}