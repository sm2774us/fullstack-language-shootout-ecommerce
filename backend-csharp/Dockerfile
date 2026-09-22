# syntax=docker/dockerfile:1
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS builder
WORKDIR /app
COPY . .
RUN dotnet publish backend-csharp.csproj -c Release -o /out

FROM mcr.microsoft.com/dotnet/aspnet:9.0
COPY --from=builder /out .
EXPOSE 8080
CMD ["dotnet", "backend-csharp.dll"]
