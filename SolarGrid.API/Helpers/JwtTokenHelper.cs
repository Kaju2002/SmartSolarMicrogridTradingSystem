/*
 * File: JwtTokenHelper.cs
 * Description: Create JWT access token after login
 * Author: Vithusha (Identity and Access)
 * Date: 22/09/2026
 */
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using SolarGrid.API.Models;

namespace SolarGrid.API.Helpers;

public class JwtTokenHelper
{
    private readonly IConfiguration _configuration;

    // Read JWT settings from config
    public JwtTokenHelper(IConfiguration configuration)
    {
        _configuration = configuration;
    }

    // Build signed token with user id, role and name
    public string GenerateToken(User user)
    {
        var key = _configuration["JwtSettings:Key"] ?? "SolarGridSecretKey_ChangeThis_Min32Chars!!";
        var issuer = _configuration["JwtSettings:Issuer"] ?? "SolarGrid.API";
        var audience = _configuration["JwtSettings:Audience"] ?? "SolarGrid.Clients";
        var expireMinutes = int.TryParse(_configuration["JwtSettings:ExpireMinutes"], out var m) ? m : 120;

        var claims = new List<Claim>
        {
            new Claim(JwtRegisteredClaimNames.Sub, user.Id ?? string.Empty),
            new Claim(ClaimTypes.NameIdentifier, user.Id ?? string.Empty),
            new Claim(ClaimTypes.Role, user.UserType),
            new Claim(ClaimTypes.Name, user.FullName),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
        };

        var securityKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key));
        var credentials = new SigningCredentials(securityKey, SecurityAlgorithms.HmacSha256);

        var token = new JwtSecurityToken(
            issuer: issuer,
            audience: audience,
            claims: claims,
            expires: DateTime.UtcNow.AddMinutes(expireMinutes),
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
