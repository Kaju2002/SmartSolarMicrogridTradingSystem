/*
 * File: RegisterRequestDto.cs
 * Description: Register new user request
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
namespace SolarGrid.API.DTOs;

public class RegisterRequestDto
{
    public string UserType { get; set; } = string.Empty;
    public string? Nic { get; set; }
    public string? Username { get; set; }
    public string Password { get; set; } = string.Empty;
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string PhoneNumber { get; set; } = string.Empty;
}
