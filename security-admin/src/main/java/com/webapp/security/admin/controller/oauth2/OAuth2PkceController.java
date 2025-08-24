package com.webapp.security.admin.controller.oauth2;

import com.webapp.security.admin.controller.oauth2.dto.PkceParamsDTO;
import com.webapp.security.admin.service.PkceService;
import com.webapp.security.core.model.ResponseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OAuth2 PKCE参数管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8081", "http://localhost:3000"}, allowCredentials = "true")
public class OAuth2PkceController {

    private final PkceService pkceService;

    /**
     * 生成PKCE参数
     * @return PKCE参数组合
     */
    @PostMapping("/generate-pkce")
    public ResponseResult<PkceParamsDTO> generatePkce() {
        try {
            Map<String, String> params = pkceService.generatePkceParams();
            
            PkceParamsDTO dto = new PkceParamsDTO();
            dto.setState(params.get("state"));
            dto.setCodeVerifier(params.get("code_verifier"));
            dto.setCodeChallenge(params.get("code_challenge"));
            dto.setCodeChallengeMethod(params.get("code_challenge_method"));
            
            log.info("Generated PKCE params for frontend, state: {}", dto.getState());
            return ResponseResult.success(dto, "PKCE参数生成成功");
            
        } catch (Exception e) {
            log.error("Failed to generate PKCE params", e);
            return ResponseResult.failed("生成PKCE参数失败: " + e.getMessage());
        }
    }

    /**
     * 根据state获取code_verifier
     * @param state OAuth2 state参数
     * @return code_verifier
     */
    @GetMapping("/get-verifier/{state}")
    public ResponseResult<String> getCodeVerifier(@PathVariable String state) {
        try {
            String codeVerifier = pkceService.getCodeVerifier(state);
            
            if (codeVerifier != null) {
                log.info("Retrieved code_verifier for state: {}", state);
                return ResponseResult.success(codeVerifier, "获取code_verifier成功");
            } else {
                log.warn("No code_verifier found for state: {}", state);
                return ResponseResult.failed("未找到对应的code_verifier，可能已过期或不存在");
            }
            
        } catch (Exception e) {
            log.error("Failed to get code_verifier for state: {}", state, e);
            return ResponseResult.failed("获取code_verifier失败: " + e.getMessage());
        }
    }

    /**
     * 验证state参数是否有效
     * @param state OAuth2 state参数
     * @return 是否有效
     */
    @GetMapping("/validate-state/{state}")
    public ResponseResult<Boolean> validateState(@PathVariable String state) {
        try {
            boolean isValid = pkceService.validateState(state);
            log.info("State validation result for {}: {}", state, isValid);
            return ResponseResult.success(isValid, isValid ? "State有效" : "State无效或已过期");
            
        } catch (Exception e) {
            log.error("Failed to validate state: {}", state, e);
            return ResponseResult.failed("验证state失败: " + e.getMessage());
        }
    }

    /**
     * 清理PKCE参数（可选接口）
     * @param state OAuth2 state参数
     * @return 清理结果
     */
    @DeleteMapping("/cleanup/{state}")
    public ResponseResult<Void> cleanupPkceParams(@PathVariable String state) {
        try {
            pkceService.cleanupPkceParams(state);
            log.info("Cleaned up PKCE params for state: {}", state);
            return ResponseResult.success(null, "清理PKCE参数成功");
            
        } catch (Exception e) {
            log.error("Failed to cleanup PKCE params for state: {}", state, e);
            return ResponseResult.failed("清理PKCE参数失败: " + e.getMessage());
        }
    }
}