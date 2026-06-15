package mathlogic_lab5;

public interface SemanticActions { // Интерфейс для семантических действий, выполняемых при применении правил
									// грамматики
	void execute(String production, String actionDescription); // Выполнить семантическое действие, связанное с
																// продукцией

	void reset(); // Сбросить состояние семантических действий

	String getResult(); // Получить результат работы семантических действий после завершения разбора
}