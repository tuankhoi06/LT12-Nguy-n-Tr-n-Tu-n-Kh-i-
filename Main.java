package Bai_1;

public class Main {
    public static void main(String[] args) {
        MathOperation add = (a, b) -> a + b;
        MathOperation subtract = (a, b) -> a - b;
        MathOperation multiply = (a, b) -> a * b;
        MathOperation divide = (a, b) -> {
            if (b == 0) {
                System.out.println("Không thể chia cho 0");
                return 0;
            }
            return a / b;
        };

        int a = 20;
        int b = 5;

        System.out.println("a = " + a + ", b = " + b);
        System.out.println("Cộng: " + add.compute(a, b));
        System.out.println("Trừ: " + subtract.compute(a, b));
        System.out.println("Nhân: " + multiply.compute(a, b));
        System.out.println("Chia: " + divide.compute(a, b));
    }
}