/*
 * File: ClaimsPrincipalExtensions.cs
 * Description: Read user id and role from JWT claims
 * Author: Team
 * Date: 23/09/2026
 */
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace SolarGrid.API.Helpers;

public static class ClaimsPrincipalExtensions
{
    // Prefer NameIdentifier / Sub from the access token
    public static string? GetUserId(this ClaimsPrincipal user)
    {
        return user.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? user.FindFirstValue(JwtRegisteredClaimNames.Sub)
            ?? user.FindFirstValue("sub");
    }

    public static bool IsBackoffice(this ClaimsPrincipal user) =>
        user.IsInRole("Backoffice");
}
