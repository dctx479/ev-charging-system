#!/bin/bash

# Token黑名单功能测试脚本

BASE_URL="http://localhost:8080/api"

echo "=== Token黑名单功能测试 ==="
echo ""

# 1. 测试用户登录
echo "1. 测试用户登录..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token')
USER_ID=$(echo $LOGIN_RESPONSE | jq -r '.data.user.id')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ 登录失败: $LOGIN_RESPONSE"
  exit 1
fi

echo "✅ 登录成功"
echo "   Token: ${TOKEN:0:50}..."
echo "   UserID: $USER_ID"
echo ""

# 2. 测试使用token访问接口
echo "2. 测试使用token访问接口..."
INFO_RESPONSE=$(curl -s -X GET "$BASE_URL/auth/user/info" \
  -H "Authorization: Bearer $TOKEN")

INFO_CODE=$(echo $INFO_RESPONSE | jq -r '.code')

if [ "$INFO_CODE" == "200" ]; then
  echo "✅ Token有效，可以访问接口"
else
  echo "❌ Token无效: $INFO_RESPONSE"
  exit 1
fi
echo ""

# 3. 测试登出功能
echo "3. 测试登出功能..."
LOGOUT_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $TOKEN")

LOGOUT_CODE=$(echo $LOGOUT_RESPONSE | jq -r '.code')

if [ "$LOGOUT_CODE" == "200" ]; then
  echo "✅ 登出成功"
else
  echo "❌ 登出失败: $LOGOUT_RESPONSE"
  exit 1
fi
echo ""

# 4. 测试登出后token失效
echo "4. 测试登出后token是否失效..."
sleep 1
AFTER_LOGOUT_RESPONSE=$(curl -s -X GET "$BASE_URL/auth/user/info" \
  -H "Authorization: Bearer $TOKEN")

AFTER_LOGOUT_CODE=$(echo $AFTER_LOGOUT_RESPONSE | jq -r '.code')

if [ "$AFTER_LOGOUT_CODE" == "401" ]; then
  echo "✅ Token已失效（黑名单生效）"
  echo "   错误信息: $(echo $AFTER_LOGOUT_RESPONSE | jq -r '.message')"
else
  echo "❌ Token应该失效但仍然有效: $AFTER_LOGOUT_RESPONSE"
  exit 1
fi
echo ""

# 5. 测试强制下线（需要管理员权限）
echo "5. 测试强制下线功能..."
echo "   注意: 此测试需要管理员账号"
echo ""

# 先登录管理员账号
echo "5.1. 管理员登录..."
ADMIN_LOGIN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

ADMIN_TOKEN=$(echo $ADMIN_LOGIN | jq -r '.data.token')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo "⚠️  管理员登录失败，跳过强制下线测试"
  echo "   (如需测试此功能，请确保有admin账号)"
else
  echo "✅ 管理员登录成功"
  
  # 普通用户重新登录
  echo ""
  echo "5.2. 普通用户重新登录..."
  USER_LOGIN=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser","password":"123456"}')
  
  USER_TOKEN=$(echo $USER_LOGIN | jq -r '.data.token')
  USER_ID=$(echo $USER_LOGIN | jq -r '.data.user.id')
  echo "✅ 普通用户登录成功, UserID: $USER_ID"
  
  # 验证token有效
  echo ""
  echo "5.3. 验证token有效..."
  BEFORE_FORCE=$(curl -s -X GET "$BASE_URL/auth/user/info" \
    -H "Authorization: Bearer $USER_TOKEN")
  
  if [ "$(echo $BEFORE_FORCE | jq -r '.code')" == "200" ]; then
    echo "✅ Token有效"
  fi
  
  # 管理员强制下线
  echo ""
  echo "5.4. 管理员强制用户下线..."
  FORCE_LOGOUT=$(curl -s -X POST "$BASE_URL/auth/admin/users/$USER_ID/force-logout" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
  
  if [ "$(echo $FORCE_LOGOUT | jq -r '.code')" == "200" ]; then
    echo "✅ 强制下线成功"
  else
    echo "❌ 强制下线失败: $FORCE_LOGOUT"
  fi
  
  # 验证用户token失效
  echo ""
  echo "5.5. 验证用户token已失效..."
  sleep 1
  AFTER_FORCE=$(curl -s -X GET "$BASE_URL/auth/user/info" \
    -H "Authorization: Bearer $USER_TOKEN")
  
  if [ "$(echo $AFTER_FORCE | jq -r '.code')" == "401" ]; then
    echo "✅ Token已失效（强制下线生效）"
    echo "   错误信息: $(echo $AFTER_FORCE | jq -r '.message')"
  else
    echo "❌ Token应该失效但仍然有效: $AFTER_FORCE"
  fi
  
  # 查看黑名单统计
  echo ""
  echo "5.6. 查看黑名单统计..."
  STATS=$(curl -s -X GET "$BASE_URL/auth/admin/blacklist/stats" \
    -H "Authorization: Bearer $ADMIN_TOKEN")
  
  echo "   黑名单统计: $(echo $STATS | jq -r '.data')"
fi

echo ""
echo "=== 测试完成 ==="
