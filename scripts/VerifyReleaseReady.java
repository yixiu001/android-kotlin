import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.Predicate;

/** Java 17 source launcher; no third-party dependencies and no Gradle daemon required. */
class VerifyReleaseReady {
    static String get(Properties p, String key) { return p.getProperty(key, "").trim(); }
    static Properties read(Path path) throws IOException {
        Properties p = new Properties();
        if (Files.exists(path)) try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(r); }
        return p;
    }
    static boolean placeholder(String value) {
        return value.isBlank() || value.matches("(?is).*(replace|placeholder|example|todo|待填写|示例|测试工程|未配置|请填写).*" );
    }
    static boolean publicHttps(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) return false;
            host = host.toLowerCase(Locale.ROOT);
            if (!host.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\\.[a-z]{2,63}")) return false;
            if (host.matches(".*(?:^|[.-])(?:test[0-9]*|testing|dev[0-9]*|development|stage|staging|demo|sandbox|localhost|placeholder|todo|replace)(?:[.-]|$).*")) return false;
            if (host.matches(".*\\.(?:local|internal|invalid|localhost|example|test)$")) return false;
            return !host.matches("(?:.*\\.)?example\\.(?:com|org|net)");
        } catch (IllegalArgumentException e) { return false; }
    }
    static String host(String value) {
        try { return Objects.toString(URI.create(value).getHost(), "").toLowerCase(Locale.ROOT); }
        catch (IllegalArgumentException e) { return ""; }
    }
    // Pure checks: injected file existence lets fixtures use no real signing material or filesystem writes.
    static List<String> validate(Properties prod, Properties test, Properties signing,
                                 boolean productionExists, Predicate<String> fileExists) {
        List<String> errors = new ArrayList<>();
        if (!productionExists) errors.add("缺少 environments/production.properties（先复制 example 并填写真实信息）。");
        for (String key : List.of("API_BASE_URL", "PRIVACY_POLICY_URL", "TERMS_URL")) {
            if (!publicHttps(get(prod, key))) errors.add(key + " 必须是公开正式 HTTPS 域名，不能是测试、localhost、IP 或示例地址。");
            if (!host(get(prod, key)).isBlank() && host(get(prod, key)).equals(host(get(test, "API_BASE_URL"))))
                errors.add(key + " 不能使用测试 API 域名。");
        }
        for (String key : List.of("FEEDBACK_URL", "APP_MARKET_URL")) {
            if (!get(prod, key).isBlank() && !publicHttps(get(prod, key))) errors.add(key + " 必须为空或公开正式 HTTPS 地址。");
        }
        for (String key : List.of("SMS_ENABLED", "WECHAT_ENABLED", "ONBOARDING_ENABLED", "APP_RECORD_REQUIRED")) {
            if (!Set.of("true", "false").contains(get(prod, key))) errors.add(key + " 必须显式为 true 或 false。");
        }
        if (!"false".equals(get(prod, "TEST_TOOLS_ENABLED"))) errors.add("TEST_TOOLS_ENABLED 必须显式为 false（release 代码也强制关闭）。");
        if (!"false".equals(get(prod, "DEMO_ENABLED"))) errors.add("DEMO_ENABLED 必须为 false；业务示例不能作为正式内容。");
        if (!"true".equals(get(prod, "RELEASE_CONTENT_READY"))) errors.add("RELEASE_CONTENT_READY 必须为 true：人工检查真实首页/业务、政策、SDK 和权限披露后确认。");
        if (!get(prod, "PRIVACY_VERSION").matches("production-[A-Za-z0-9][A-Za-z0-9._-]*"))
            errors.add("PRIVACY_VERSION 必须以 production- 开头并包含正式政策版本。");
        if (get(prod, "PRIVACY_VERSION").equals(get(test, "PRIVACY_VERSION"))) errors.add("正式与测试隐私版本不能相同。");
        if ("true".equals(get(prod, "ONBOARDING_ENABLED")) && placeholder(get(prod, "ONBOARDING_VERSION"))) errors.add("开启引导时必须配置 ONBOARDING_VERSION。");
        if (placeholder(get(prod, "OPERATOR_NAME"))) errors.add("OPERATOR_NAME 必须填写真实运营主体。");
        String email = get(prod, "SUPPORT_EMAIL");
        if (!email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}")
                || !publicHttps("https://" + email.substring(email.lastIndexOf('@') + 1))) errors.add("SUPPORT_EMAIL 必须填写正式有效联系邮箱。");
        if ("true".equals(get(prod, "APP_RECORD_REQUIRED")) && placeholder(get(prod, "APP_RECORD_NUMBER"))) errors.add("声明需要备案时必须填写真实 APP_RECORD_NUMBER；真实性须人工审核。");
        if ("false".equals(get(prod, "APP_RECORD_REQUIRED")) && placeholder(get(prod, "APP_RECORD_EXEMPTION_REASON"))) errors.add("声明备案不适用时必须填写 APP_RECORD_EXEMPTION_REASON，并人工核实。");
        if ("true".equals(get(prod, "WECHAT_ENABLED"))) {
            if (!get(prod, "WECHAT_APP_ID").matches("wx[0-9a-fA-F]{16}")) errors.add("微信启用时必须配置正式 WECHAT_APP_ID。");
        }
        for (String key : List.of("storeFile", "storePassword", "keyAlias", "keyPassword")) {
            if (placeholder(get(signing, key))) errors.add("keystore.properties 缺少真实 " + key + "。");
        }
        if (!fileExists.test(get(signing, "storeFile"))) errors.add("正式签名 storeFile 不存在或不是普通文件。");
        if (get(signing, "storeFile").toLowerCase(Locale.ROOT).endsWith("debug.keystore")
                || "androiddebugkey".equalsIgnoreCase(get(signing, "keyAlias"))) errors.add("正式签名不能使用 Android debug keystore。");
        return errors;
    }
    static void verifySigning(Path root, Properties signing) throws Exception {
        Path path = root.resolve(get(signing, "storeFile")).normalize();
        KeyStore store = KeyStore.getInstance(path.toFile(), signing.getProperty("storePassword").toCharArray());
        String alias = signing.getProperty("keyAlias");
        if (!store.isKeyEntry(alias) || !(store.getKey(alias, signing.getProperty("keyPassword").toCharArray()) instanceof PrivateKey))
            throw new GeneralSecurityException("配置别名没有可读私钥");
        java.security.cert.Certificate certificate = store.getCertificate(alias);
        if (certificate instanceof java.security.cert.X509Certificate x509) {
            x509.checkValidity();
            if (x509.getSubjectX500Principal().getName().contains("CN=Android Debug")) throw new GeneralSecurityException("不能使用 debug 证书");
        }
    }
    static Properties fixture() {
        Properties p = new Properties();
        p.setProperty("API_BASE_URL", "https://api.release-fixture.org");
        p.setProperty("PRIVACY_POLICY_URL", "https://legal.release-fixture.org/privacy");
        p.setProperty("TERMS_URL", "https://legal.release-fixture.org/terms");
        p.setProperty("OPERATOR_NAME", "Fixture Operator");
        p.setProperty("SUPPORT_EMAIL", "support@release-fixture.org");
        p.setProperty("PRIVACY_VERSION", "production-1");
        p.setProperty("ONBOARDING_VERSION", "1");
        for (String key : List.of("SMS_ENABLED", "WECHAT_ENABLED", "ONBOARDING_ENABLED", "APP_RECORD_REQUIRED", "TEST_TOOLS_ENABLED", "DEMO_ENABLED")) p.setProperty(key, "false");
        p.setProperty("RELEASE_CONTENT_READY", "true");
        p.setProperty("APP_RECORD_EXEMPTION_REASON", "Fixture jurisdiction assessment only");
        return p;
    }
    static void selfTest() {
        Properties signing = new Properties();
        signing.setProperty("storeFile", "fixture-only.jks");
        signing.setProperty("storePassword", "fixture-secret");
        signing.setProperty("keyPassword", "fixture-secret");
        signing.setProperty("keyAlias", "fixture");
        Properties test = new Properties();
        test.setProperty("API_BASE_URL", "https://hyperf.tongyiwl.bond");
        test.setProperty("PRIVACY_VERSION", "test-1");
        test.setProperty("WECHAT_APP_ID", "wx57879d4692d97d56");
        int count = 0;
        if (!validate(fixture(), test, signing, true, ignored -> true).isEmpty()) throw new AssertionError("valid fixture rejected");
        count++;
        Properties enabled = fixture();
        enabled.setProperty("SMS_ENABLED", "true");
        enabled.setProperty("WECHAT_ENABLED", "true");
        enabled.setProperty("WECHAT_APP_ID", test.getProperty("WECHAT_APP_ID"));
        enabled.setProperty("ONBOARDING_ENABLED", "true");
        if (!validate(enabled, test, signing, true, ignored -> true).isEmpty()) throw new AssertionError("explicit feature configuration rejected"); count++;
        enabled.setProperty("ONBOARDING_VERSION", "");
        if (validate(enabled, test, signing, true, ignored -> true).isEmpty()) throw new AssertionError("unversioned onboarding accepted"); count++;
        Properties withRecord = fixture();
        withRecord.setProperty("APP_RECORD_REQUIRED", "true");
        withRecord.setProperty("APP_RECORD_NUMBER", "fixture-record-only");
        if (!validate(withRecord, test, signing, true, ignored -> true).isEmpty()) throw new AssertionError("record presence rejected"); count++;
        String[][] invalid = {
            {"API_BASE_URL", "https://hyperf.tongyiwl.bond"}, {"API_BASE_URL", "http://api.release-fixture.org"},
            {"API_BASE_URL", "https://localhost"}, {"API_BASE_URL", "https://127.0.0.1"},
            {"API_BASE_URL", "https://[::1]"}, {"API_BASE_URL", "https://test.release-fixture.org"},
            {"API_BASE_URL", "https://api.example.com"}, {"API_BASE_URL", "https://user:secret@api.release-fixture.org"},
            {"API_BASE_URL", "https://placeholder.release-fixture.org"}, {"API_BASE_URL", "https://test2.release-fixture.org"},
            {"PRIVACY_POLICY_URL", ""}, {"TERMS_URL", "https://legal.internal"},
            {"PRIVACY_VERSION", "test-1"}, {"OPERATOR_NAME", "待填写"}, {"SUPPORT_EMAIL", "invalid"},
            {"APP_RECORD_REQUIRED", ""}, {"APP_RECORD_REQUIRED", "true"}, {"APP_RECORD_EXEMPTION_REASON", ""},
            {"TEST_TOOLS_ENABLED", "true"}, {"DEMO_ENABLED", "true"}, {"RELEASE_CONTENT_READY", "false"},
            {"SMS_ENABLED", "yes"}, {"WECHAT_ENABLED", "true"}, {"ONBOARDING_ENABLED", "yes"},
            {"APP_MARKET_URL", "http://release-fixture.org"}, {"FEEDBACK_URL", "https://192.168.1.1"}
        };
        for (String[] item : invalid) {
            Properties p = fixture(); p.setProperty(item[0], item[1]);
            if (validate(p, test, signing, true, ignored -> true).isEmpty()) throw new AssertionError("accepted " + Arrays.toString(item));
            count++;
        }
        if (validate(fixture(), test, signing, false, ignored -> true).isEmpty()) throw new AssertionError("missing production accepted"); count++;
        if (validate(fixture(), test, signing, true, ignored -> false).isEmpty()) throw new AssertionError("missing signing file accepted"); count++;
        if (validate(fixture(), test, new Properties(), true, ignored -> true).isEmpty()) throw new AssertionError("missing signing properties accepted"); count++;
        signing.setProperty("keyAlias", "androiddebugkey");
        if (validate(fixture(), test, signing, true, ignored -> true).isEmpty()) throw new AssertionError("debug signing accepted"); count++;
        System.out.println("PASS: " + count + " release-validation fixtures; no network, signing keys or APKs used.");
    }
    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        if (args.length == 1 && "--self-test".equals(args[0])) { selfTest(); return; }
        Path root = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        Path productionFile = root.resolve("environments/production.properties");
        Properties prod = read(productionFile), test = read(root.resolve("environments/test.properties")), signing = read(root.resolve("keystore.properties"));
        List<String> errors = validate(prod, test, signing, Files.isRegularFile(productionFile), value -> {
            try { return !value.isBlank() && Files.isRegularFile(root.resolve(value)); } catch (InvalidPathException e) { return false; }
        });
        if (errors.isEmpty()) {
            try { verifySigning(root, signing); }
            catch (Exception e) { errors.add("正式签名无法读取有效私钥/证书；检查文件、密码、别名和有效期（不输出凭证）。"); }
        }
        if (!errors.isEmpty()) {
            System.err.println("Release blocked / 正式发布检查未通过：");
            errors.forEach(error -> System.err.println(" - " + error));
            System.exit(1);
        }
        System.out.println("Release readiness checks passed. 主体/备案真实性、政策内容与真实业务仍需人工审核。");
    }
}
