/*
 * File: UpdateUserStatusDto.cs
 * Description: Approve / deactivate user status
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
namespace SolarGrid.API.DTOs;

public class UpdateUserStatusDto
{
    public string UserId { get; set; } = string.Empty;
    public string NewStatus { get; set; } = string.Empty;
    // Active | Deactivated
}
