/*
 * File: LoginResponseDto.cs
 * Description: Login / auth action response
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
namespace SolarGrid.API.DTOs;

public class LoginResponseDto
{
    public bool Success { get; set; }

    public string Message { get; set; } = string.Empty;

    public string? UserType { get; set; }

    public string? UserId { get; set; }

    public string? FullName { get; set; }

    public string? Token { get; set; }
    // JWT access token
}
