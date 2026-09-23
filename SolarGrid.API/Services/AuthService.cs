/*
 * File: AuthService.cs
 * Description: Login, register, profile and user status
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Helpers;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class AuthService : IAuthService
{
    private readonly IMongoCollection<User> _users;
    private readonly JwtTokenHelper _jwtTokenHelper;

    // Setup Users collection and JWT helper
    public AuthService(MongoDbContext dbContext, JwtTokenHelper jwtTokenHelper)
    {
        _users = dbContext.Database.GetCollection<User>("Users");
        _jwtTokenHelper = jwtTokenHelper;
    }

    // Login with NIC or username and check password
    public async Task<LoginResponseDto> LoginAsync(LoginRequestDto request)
    {
        var filter = Builders<User>.Filter.Or(
            Builders<User>.Filter.Eq(u => u.Nic, request.Identifier),
            Builders<User>.Filter.Eq(u => u.Username, request.Identifier)
        );

        var user = await _users.Find(filter).FirstOrDefaultAsync();

        if (user is null)
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "Invalid credentials"
            };
        }

        var passwordValid = BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash);

        if (!passwordValid)
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "Invalid credentials"
            };
        }

        if (user.Status != "Active")
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = $"Account is {user.Status}. Please contact Backoffice."
            };
        }

        var token = _jwtTokenHelper.GenerateToken(user);

        return new LoginResponseDto
        {
            Success = true,
            Message = "Login successful",
            UserType = user.UserType,
            UserId = user.Id,
            FullName = user.FullName,
            Token = token
        };
    }

    // Public self-register — Prosumer only (PendingApproval)
    public async Task<LoginResponseDto> RegisterAsync(RegisterRequestDto request)
    {
        if (!string.IsNullOrWhiteSpace(request.UserType)
            && !string.Equals(request.UserType, "Prosumer", StringComparison.OrdinalIgnoreCase))
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "Public registration is limited to Prosumer accounts"
            };
        }

        return await CreateUserAsync(
            userType: "Prosumer",
            status: "PendingApproval",
            nic: request.Nic,
            username: request.Username,
            password: request.Password,
            fullName: request.FullName,
            email: request.Email,
            phoneNumber: request.PhoneNumber);
    }

    // Backoffice creates an Active Grid Operator
    public async Task<LoginResponseDto> CreateGridOperatorAsync(RegisterRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.Username))
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "Username is required for Grid Operator"
            };
        }

        return await CreateUserAsync(
            userType: "GridOperator",
            status: "Active",
            nic: null,
            username: request.Username,
            password: request.Password,
            fullName: request.FullName,
            email: request.Email,
            phoneNumber: request.PhoneNumber);
    }

    // Shared insert with uniqueness checks
    private async Task<LoginResponseDto> CreateUserAsync(
        string userType,
        string status,
        string? nic,
        string? username,
        string password,
        string fullName,
        string email,
        string phoneNumber)
    {
        var filters = new List<FilterDefinition<User>>();

        if (!string.IsNullOrWhiteSpace(username))
            filters.Add(Builders<User>.Filter.Eq(u => u.Username, username));

        if (!string.IsNullOrWhiteSpace(nic))
            filters.Add(Builders<User>.Filter.Eq(u => u.Nic, nic));

        if (filters.Count > 0)
        {
            var existing = await _users.Find(Builders<User>.Filter.Or(filters)).FirstOrDefaultAsync();
            if (existing is not null)
            {
                return new LoginResponseDto
                {
                    Success = false,
                    Message = "Username or NIC already exists"
                };
            }
        }

        var newUser = new User
        {
            UserType = userType,
            Nic = nic,
            Username = username,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(password),
            FullName = fullName,
            Email = email,
            PhoneNumber = phoneNumber,
            Status = status,
            CreatedAt = DateTime.UtcNow
        };

        await _users.InsertOneAsync(newUser);

        return new LoginResponseDto
        {
            Success = true,
            Message = "User registered",
            UserId = newUser.Id,
            UserType = newUser.UserType,
            FullName = newUser.FullName
        };
    }

    // Change account status (approve / deactivate / reactivate)
    public async Task<LoginResponseDto> UpdateUserStatusAsync(UpdateUserStatusDto request)
    {
        var filter = Builders<User>.Filter.Eq(u => u.Id, request.UserId);
        var user = await _users.Find(filter).FirstOrDefaultAsync();

        if (user is null)
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "User not found"
            };
        }

        var update = Builders<User>.Update
            .Set(u => u.Status, request.NewStatus)
            .Set(u => u.UpdatedAt, DateTime.UtcNow);

        await _users.UpdateOneAsync(filter, update);

        return new LoginResponseDto
        {
            Success = true,
            Message = $"Status updated to {request.NewStatus}",
            UserId = user.Id,
            UserType = user.UserType,
            FullName = user.FullName
        };
    }

    // List users waiting for Backoffice approval
    public async Task<List<User>> GetPendingUsersAsync()
    {
        var filter = Builders<User>.Filter.Eq(u => u.Status, "PendingApproval");
        var users = await _users.Find(filter).ToListAsync();
        foreach (var user in users)
            user.PasswordHash = string.Empty;
        return users;
    }

    // List Grid Operators and Prosumers for Backoffice Users page
    public async Task<List<User>> GetUsersAsync(string? userType = null)
    {
        FilterDefinition<User> filter;

        if (!string.IsNullOrWhiteSpace(userType))
        {
            filter = Builders<User>.Filter.Eq(u => u.UserType, userType);
        }
        else
        {
            filter = Builders<User>.Filter.In(
                u => u.UserType,
                new[] { "GridOperator", "Prosumer" }
            );
        }

        var users = await _users.Find(filter).SortByDescending(u => u.CreatedAt).ToListAsync();
        foreach (var user in users)
            user.PasswordHash = string.Empty;
        return users;
    }

    // Get one user profile by id (never return password hash)
    public async Task<User?> GetProfileAsync(string userId)
    {
        var filter = Builders<User>.Filter.Eq(u => u.Id, userId);
        var user = await _users.Find(filter).FirstOrDefaultAsync();
        if (user is not null)
            user.PasswordHash = string.Empty;
        return user;
    }

    // Update name, email and phone only
    public async Task<LoginResponseDto> UpdateProfileAsync(string userId, UpdateProfileDto request)
    {
        var filter = Builders<User>.Filter.Eq(u => u.Id, userId);
        var user = await _users.Find(filter).FirstOrDefaultAsync();

        if (user is null)
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "User not found"
            };
        }

        var update = Builders<User>.Update
            .Set(u => u.FullName, request.FullName)
            .Set(u => u.Email, request.Email)
            .Set(u => u.PhoneNumber, request.PhoneNumber)
            .Set(u => u.UpdatedAt, DateTime.UtcNow);

        await _users.UpdateOneAsync(filter, update);

        return new LoginResponseDto
        {
            Success = true,
            Message = "Profile updated",
            UserId = user.Id,
            UserType = user.UserType,
            FullName = request.FullName
        };
    }
}
