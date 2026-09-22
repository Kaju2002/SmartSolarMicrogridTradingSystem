/*
 * File: LoginRequestDto.cs
 * Description: Login request body
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
namespace SolarGrid.API.DTOs;

public class LoginRequestDto
{
    public string Identifier { get; set; } = string.Empty;
    // NIC or Username

    public string Password { get; set; } = string.Empty;
}
