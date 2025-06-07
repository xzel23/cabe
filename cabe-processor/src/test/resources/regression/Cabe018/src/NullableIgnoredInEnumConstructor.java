import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NullableIgnoredInEnumConstructor {
    public enum AsymmetricAlgorithm {
        /**
         * RSA (Rivest-Shamir-Adleman) algorithm with OAEP padding
         */
        RSA("RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING", "SHA256withRSA"),
        /**
         * Elliptic Curve Cryptography
         */
        EC("ECIES", "SHA256withECDSA"),
        /**
         * Elliptic Curve Integrated Encryption Scheme
         */
        ECIES("ECIES", null),
        /**
         * Digital Signature Algorithm (for signatures only)
         */
        DSA("DSA", "SHA256withDSA");

        private final String transformation;
        private final @Nullable String signatureAlgorithm;

        AsymmetricAlgorithm(String transformation, @Nullable String signatureAlgorithm) {
            this.transformation = transformation;
            this.signatureAlgorithm = signatureAlgorithm;
        }

        /**
         * Retrieves the name of the algorithm represented by this instance.
         *
         * @return the name of the algorithm as a string
         */
        public String algorithm() {
            return name();
        }

        /**
         * Retrieves the signature algorithm associated with this asymmetric algorithm, if available.
         *
         * @return an {@code Optional} containing the signature algorithm as a string, or an empty
         *         {@code Optional} if no signature algorithm is defined.
         */
        public Optional<String> getSignatureAlgorithm() {
            return Optional.ofNullable(signatureAlgorithm);
        }

        /**
         * Retrieves the transformation string associated with the asymmetric algorithm.
         *
         * @return the transformation string, which specifies the cipher transformation
         *         used by this asymmetric algorithm
         */
        public String transformation() {
            return transformation;
        }
    }

    public static void main(String[] args) {
        AsymmetricAlgorithm.values();
    }
}