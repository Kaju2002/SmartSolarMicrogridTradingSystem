using MongoDB.Bson;
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// MongoDB DI
builder.Services.AddSingleton<MongoDbContext>();

// Auth service DI
builder.Services.AddScoped<IAuthService, AuthService>();

// Reservation service DI
builder.Services.AddScoped<IReservationService, ReservationService>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.MapControllers();

// Temporary: MongoDB ping test
app.MapGet("/ping-db", async (MongoDbContext db) =>
{
    var result = await db.Database.RunCommandAsync<BsonDocument>(new BsonDocument("ping", 1));
    return Results.Ok(new { status = "MongoDB connected" });
});

app.Run();