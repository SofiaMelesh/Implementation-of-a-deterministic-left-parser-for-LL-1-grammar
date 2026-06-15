package mathlogic_lab5;

import java.util.Scanner;
import java.util.Random;

public class Main {
	public static void main(String[] args) {
		System.out.println("LL(1) анализатор для грамматики: S -> a B, B -> a B B | b");

		GrammarAnalyzer parser = new GrammarAnalyzer(); // создаём объект анализатора
		Scanner scanner = new Scanner(System.in); // для чтения ввода с клавиатуры
		Random random = new Random(); // для случайной генерации

		while (true) { // бесконечный цикл меню
			System.out.println("\n   Меню:");
			System.out.println("1. Проверить строку");
			System.out.println("2. Сгенерировать случайную строку");
			System.out.println("3. Показать множества FIRST и FOLLOW");
			System.out.println("4. Показать LL(1) таблицу разбора");
			System.out.println("5. Выход");
			System.out.print("Ваш выбор: ");

			String choice = scanner.nextLine().trim(); // читаем выбор пользователя
			switch (choice) {
			case "1":
				System.out.print("Введите строку (только 'a' и 'b'): ");
				String input = scanner.nextLine().trim();
				if (input.matches("[ab]*")) { // проверка, что только a и b
					boolean valid = parser.parse(input);
					System.out.println("Результат: " + (valid ? "КОРРЕКТНАЯ строка" : "НЕКОРРЕКТНАЯ строка"));
				} else {
					System.out.println("Недопустимые символы для грамматики. Используйте только 'a' и 'b'.");
				}
				break;
			case "2":
				String generated = parser.generate(random);
				System.out.println("Сгенерированная строка: " + generated);
				parser.parse(generated); // показать лог разбора и семантические действия
				break;
			case "3":
				parser.printFirstAndFollow();
				break;
			case "4":
				parser.printParsingTable();
				break;
			case "5":
				System.out.println("До новых встреч!");
				scanner.close();
				System.exit(0);
				break;
			default:
				System.out.println("Неверный выбор.");
			}
		}
	}
}