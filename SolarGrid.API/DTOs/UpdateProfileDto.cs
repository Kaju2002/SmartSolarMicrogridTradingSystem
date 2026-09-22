/*
 * File: UpdateProfileDto.cs
 * Description: Update profile fields only
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
namespace SolarGrid.API.DTOs;

public class UpdateProfileDto
{
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string PhoneNumber { get; set; } = string.Empty;
}
