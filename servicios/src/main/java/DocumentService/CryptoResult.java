package DocumentService;

public class CryptoResult {
    private final String hashResult;
    private final String finalEncryptedPath;

    public CryptoResult(String hashResult, String finalEncryptedPath) {
        this.hashResult = hashResult;
        this.finalEncryptedPath = finalEncryptedPath;
    }

    public String getHashResult() {
        return hashResult;
    }

    public String getFinalEncryptedPath() {
        return finalEncryptedPath;
    }
}