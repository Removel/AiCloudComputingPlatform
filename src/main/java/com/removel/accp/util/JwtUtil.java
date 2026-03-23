package com.removel.accp.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class JwtUtil {
    // （烂俗笑话）密钥：天雷滚滚我好怕怕，劈得我浑身掉渣渣；突破天劫我笑哈哈，逆天改命我吹喇叭，嘀嗒嘀嗒滴滴答。
    private static final String SECRET_KEY = "5aSp6Zu35rua5rua5oiR5aW95oCV5oCV77yM5YqI5b6X5oiR5rWR6Lqr5o6J5rij5rij77yb56qB56C05aSp5Yqr5oiR56yR5ZOI5ZOI77yM6YCG5aSp5pS55ZG95oiR5ZC55ZaH5Y+t77yM5ZiA5ZeS5ZiA5ZeS5ru05ru0562U44CC";
    //注意：
    // 1、这里的密钥是base64编码，可以根据需求改动你的密钥，可以直接写
    // 2、但是不推荐这么直接写，建议将密钥存储在服务器的环境变量中，然后通过配置中心获取密钥
    // 3、密钥长度根据包建议是密钥字符串必须≥32个字节（256位）

    // 过期时间：12小时，单位毫秒 (12*60*60*1000)
    private static final long EXPIRATION_TIME = 43200000L;

    private static final SecretKey KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));

    /**
     * 生成JWT令牌
     * @param claimsMap 自定义载荷信息（要存入JWT的键值对）
     * @param name 指定来自的用户实体类，用于指定用户名以为了设定令牌主题
     * @return 生成的JWT令牌字符串
     */
    public static String generateJwtToken(Map<String, Object> claimsMap, String name) {
        return Jwts.builder()   //开始构建jwt
                .subject(name)    //根据传入的user的name设定令牌主题（用户名）
                .issuer("service-114514")   //签发者
                .issuedAt(new Date())   //签发时间
                .expiration(new Date(System.currentTimeMillis()+EXPIRATION_TIME))   //过期时间
                .id(UUID.randomUUID().toString())   //JWT id，防止被滥用
                .claims(claimsMap)  //自定义信息段，通过claims传入进来
                .signWith(KEY)  //使用密钥签名
                .compact(); //构建结束
    }

    /**
     * 新的包修改了新的api，bro是真不会了，只能靠ai帮忙了
     * ========== 核心方法：验证并解析令牌 ==========
     * 一次性完成：验证签名 + 验证过期 + 解析数据
     *
     * @param token JWT令牌
     * @return Claims 令牌中包含的所有数据
     * @throws JwtException 验证失败时抛出异常
     */
    public static Claims validateAndParseJwtToken(String token) throws JwtException {

        if(token == null || token.trim().isEmpty()){
            throw new JwtException("令牌不能为空");
        }
        try {
            // 2. 创建解析器并设置验证密钥
            JwtParser parser = Jwts.parser()
                    .verifyWith(KEY)  // 设置密钥用于验证签名
                    .build();

            // 3. 解析并验证token（这一步会同时验证签名和过期时间）
            //    parseSignedClaims 方法会：
            //    - 验证签名是否被篡改
            //    - 检查是否过期
            //    - 检查格式是否正确
            Jws<Claims> jws = parser.parseSignedClaims(token);

            // 4. 获取Claims（令牌中的数据）
            Claims claims = jws.getPayload();

            // 5. 额外的自定义验证（可选）
            //    比如验证签发者是否正确
            String issuer = claims.getIssuer();
            if (issuer != null && !"service-114514".equals(issuer)) {
                throw new JwtException("非法的签发者: " + issuer);
            }

            //返回载荷内容
            return claims;

        } catch (ExpiredJwtException e) {
            // token已过期
            throw new JwtException("Token已过期", e);
        } catch (UnsupportedJwtException e) {
            // 不支持的JWT格式
            throw new JwtException("不支持的Token格式", e);
        } catch (MalformedJwtException e) {
            // JWT格式错误
            throw new JwtException("Token格式错误", e);
        } catch (SignatureException e) {
            // 签名验证失败（可能被篡改或密钥错误）
            throw new JwtException("Token签名验证失败", e);
        } catch (IllegalArgumentException e) {
            // 参数错误
            throw new JwtException("Token参数错误", e);
        } catch (Exception e) {
            // 其他未知错误
            throw new JwtException("Token验证过程中发生未知错误", e);
        }


    }
}
