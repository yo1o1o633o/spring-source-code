import org.springframework.boot.SpringApplication;

/**
 * @author shuai.yang
 */
public class ServerApplicationBoot {
    public static void main(String[] args) {
        int a = 8;
        int shift = 0;
        int value = 1;
        while (value < a && value < 3000) {
            value <<= 1;
            shift++;
        }
        System.out.println(shift);

        int size = 1 << shift;
        System.out.println(size);

        long n = (16 + size - 1L) / size;
        System.out.println(n);
//        SpringApplication.run(ServerApplicationBoot.class, args);
    }
}
