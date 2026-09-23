/*
 * File: Program.cs
 * Description: App startup, DI and middleware
 * Author: Team
 * Date: 20/09/2026
 */
using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using MongoDB.Bson;
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.Helpers;
using SolarGrid.API.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();

// Swagger with JWT Bearer button
builder.Services.AddSwaggerGen(options =>
{
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "Bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Enter JWT token from login response"
    });

    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

// Allow Web and Mobile clients to call this API
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowClients", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

// JWT authentication
var jwtKey = builder.Configuration["JwtSettings:Key"] ?? "SolarGridSecretKey_ChangeThis_Min32Chars!!";
var jwtIssuer = builder.Configuration["JwtSettings:Issuer"] ?? "SolarGrid.API";
var jwtAudience = builder.Configuration["JwtSettings:Audience"] ?? "SolarGrid.Clients";

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = jwtIssuer,
            ValidAudience = jwtAudience,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)),
            RoleClaimType = System.Security.Claims.ClaimTypes.Role,
            NameClaimType = System.Security.Claims.ClaimTypes.Name,
            ClockSkew = TimeSpan.Zero
        };
    });

builder.Services.AddAuthorization();

// MongoDB DI
builder.Services.AddSingleton<MongoDbContext>();

// JWT helper DI
builder.Services.AddSingleton<JwtTokenHelper>();

// Auth service DI
builder.Services.AddScoped<IAuthService, AuthService>();

// Reservation service DI
builder.Services.AddScoped<IReservationService, ReservationService>();

// Station service DI
builder.Services.AddScoped<IStationService, StationService>();

// Verification service DI
builder.Services.AddScoped<IVerificationService, VerificationService>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseCors("AllowClients");

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

// Temporary: MongoDB ping test
app.MapGet("/ping-db", async (MongoDbContext db) =>
{
    var result = await db.Database.RunCommandAsync<BsonDocument>(new BsonDocument("ping", 1));
    return Results.Ok(new { status = "MongoDB connected" });
});

app.Run();
