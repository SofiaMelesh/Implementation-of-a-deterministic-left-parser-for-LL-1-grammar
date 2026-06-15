package mathlogic_lab5;

import java.util.*;

public class GrammarAnalyzer {

    private final Map<String, List<List<String>>> productions; // правила: нетерминал -> список правых частей
    private final String startSymbol;                          // начальный символ
    private final Map<String, Set<String>> firstSets;          // FIRST множества
    private final Map<String, Set<String>> followSets;         // FOLLOW множества
    private final Map<String, Map<String, String>> parsingTable; // LL(1) таблица
    private final SemanticActions semanticAction;              // семантические действия (новые)

    // Константы грамматики
    private static final String[] NONTERMINALS = { "S", "B" };
    private static final String[] TERMINALS = { "a", "b", "$" };
    private static final String START = "S";


    public GrammarAnalyzer() {
        // определяем продукции
        productions = new LinkedHashMap<>();
        productions.put("S", Arrays.asList(Arrays.asList("a", "B")));
        productions.put("B", Arrays.asList(
                Arrays.asList("a", "B", "B"),
                Arrays.asList("b")
        ));
        startSymbol = START;
        firstSets = new HashMap<>();
        followSets = new HashMap<>();
        parsingTable = new HashMap<>();

        // семантика действия
        semanticAction = new SemanticActions() {
            // инструкции
            private final List<String> instructions = new ArrayList<>();

            @Override
            public void execute(String production, String actionDescription) {
                // Для наглядности выводим пояснение и саму инструкцию
                System.out.print("  [Генерация кода] " + actionDescription + " → ");
                switch (production) {
                    case "S -> a B":
                        // Правило S: начало вычисления – помещаем начальное значение 1
                        instructions.add("PUSH 1");
                        System.out.println("PUSH 1");
                        break;
                    case "B -> a B B":
                        // Правило B -> a B B: дублируем верхнее значение стека (имитация умножения или сложения)
                        instructions.add("DUP");
                        System.out.println("DUP");
                        break;
                    case "B -> b":
                        // Правило B -> b: помещаем константу 2
                        instructions.add("PUSH 2");
                        System.out.println("PUSH 2");
                        break;
                    default:
                        System.out.println("(нет инструкции)");
                        break;
                }
            }

            @Override
            public void reset() {
                instructions.clear(); // очищаем сгенерированный код перед новым разбором
            }

            @Override
            public String getResult() {
                // Возвращаем текст программы
                return "Сгенерированная программа:\n" + String.join("\n", instructions);
            }
        };

        // 3. Вычисляем множества и таблицу
        computeFirstSets();
        computeFollowSets();
        buildParsingTable();
    }

  
    private void computeFirstSets() {
        // FIRST для терминалов
        for (String t : TERMINALS) {
            Set<String> set = new HashSet<>();
            if (!t.equals("$")) {
                set.add(t);
            }
            firstSets.put(t, set);
        }
        // FIRST для нетерминалов – пустые
        for (String nt : NONTERMINALS) {
            firstSets.put(nt, new HashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (String nt : NONTERMINALS) {
                for (List<String> rhs : productions.get(nt)) {
                    Set<String> firstRhs = firstOfSequence(rhs);
                    if (firstSets.get(nt).addAll(firstRhs)) {
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    /**
     * Вычисляет FIRST для последовательности символов.
     */
    private Set<String> firstOfSequence(List<String> seq) {
        Set<String> result = new HashSet<>();
        boolean allHaveEpsilon = true;
        for (String sym : seq) {
            Set<String> firstSym = firstSets.get(sym);
            if (firstSym == null) { // терминал
                result.add(sym);
                allHaveEpsilon = false;
                break;
            } else {
                boolean hasEpsilon = firstSym.contains("ε");
                for (String f : firstSym) {
                    if (!f.equals("ε")) result.add(f);
                }
                if (!hasEpsilon) {
                    allHaveEpsilon = false;
                    break;
                }
            }
        }
        if (allHaveEpsilon) {
            result.add("ε");
        }
        return result;
    }

    /**
     * Вычисляет FOLLOW множества для нетерминалов.
     */
    private void computeFollowSets() {
        for (String nt : NONTERMINALS) {
            followSets.put(nt, new HashSet<>());
        }
        followSets.get(startSymbol).add("$");

        boolean changed;
        do {
            changed = false;
            for (String nt : NONTERMINALS) {
                for (List<String> rhs : productions.get(nt)) {
                    for (int i = 0; i < rhs.size(); i++) {
                        String sym = rhs.get(i);
                        if (isNonterminal(sym)) {
                            List<String> beta = rhs.subList(i + 1, rhs.size());
                            Set<String> firstBeta = firstOfSequence(beta);
                            boolean epsilonInFirst = firstBeta.contains("ε");

                            // Добавляем FIRST(beta) без ε
                            for (String term : firstBeta) {
                                if (!term.equals("ε")) {
                                    if (followSets.get(sym).add(term)) changed = true;
                                }
                            }
                            // Если ε есть в FIRST(beta) или beta пусто, добавляем FOLLOW(nt)
                            if (epsilonInFirst || beta.isEmpty()) {
                                if (followSets.get(sym).addAll(followSets.get(nt))) changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
    }

    private boolean isNonterminal(String sym) {
        return sym.equals("S") || sym.equals("B");
    }

    /**
     * Строит LL(1) таблицу разбора.
     */
    private void buildParsingTable() {
        for (String nt : NONTERMINALS) {
            parsingTable.put(nt, new HashMap<>());
            for (List<String> rhs : productions.get(nt)) {
                Set<String> firstRhs = firstOfSequence(rhs);
                // Для каждого терминала в FIRST(rhs) добавляем правило
                for (String terminal : firstRhs) {
                    if (!terminal.equals("ε")) {
                        if (parsingTable.get(nt).containsKey(terminal)) {
                            System.err.println("Предупреждение: конфликт для " + nt + " на символе " + terminal);
                        }
                        parsingTable.get(nt).put(terminal, nt + " -> " + String.join(" ", rhs));
                    }
                }
                // Если есть ε, то для всех терминалов из FOLLOW(nt)
                if (firstRhs.contains("ε")) {
                    for (String terminal : followSets.get(nt)) {
                        if (parsingTable.get(nt).containsKey(terminal)) {
                            System.err.println("Предупреждение: конфликт для " + nt + " на символе " + terminal);
                        }
                        parsingTable.get(nt).put(terminal, nt + " -> ε");
                    }
                }
            }
        }
    }

    // ---------------------- Публичные методы ---------------------------------

    /**
     * Запускает LL(1) разбор входной строки.
     *
     * @param input строка из терминалов a,b
     * @return true, если строка принадлежит языку, иначе false
     */
    public boolean parse(String input) {
        semanticAction.reset(); // очищаем сгенерированный код
        System.out.println("\n   Разбор строки: \"" + input + "\"");

        // Подготавливаем входную ленту
        List<String> tokens = new ArrayList<>();
        for (char c : input.toCharArray()) tokens.add(String.valueOf(c));
        tokens.add("$"); // маркер конца

        Deque<String> stack = new ArrayDeque<>();
        stack.push("$");
        stack.push(startSymbol);
        int idx = 0;
        boolean accept = false;

        System.out.println("Стек\t\tТекущий символ\tДействие");

        while (!stack.isEmpty()) {
            String top = stack.peek();
            String lookahead = tokens.get(idx);
            System.out.print(stack.toString() + "\t\t" + lookahead + "\t\t");

            // Случай 1: вершина стека совпадает с текущим символом входа
            if (top.equals(lookahead)) {
                System.out.println("Совпадение " + top);
                stack.pop();
                idx++;
            }
            // Случай 2: вершина – нетерминал, ищем правило в таблице
            else if (isNonterminal(top)) {
                Map<String, String> row = parsingTable.get(top);
                if (row != null && row.containsKey(lookahead)) {
                    String production = row.get(lookahead);
                    System.out.println("Применить: " + production);
                    stack.pop();
                    // Если правило не ε, помещаем правую часть в стек (в обратном порядке)
                    if (!production.endsWith("ε")) {
                        String rhs = production.split(" -> ")[1];
                        if (!rhs.equals("ε")) {
                            String[] symbols = rhs.split(" ");
                            for (int i = symbols.length - 1; i >= 0; i--) {
                                stack.push(symbols[i]);
                            }
                        }
                    }
                    // Определяем текстовое описание для семантического действия
                    String actionDescr = "";
                    if (production.equals("S -> a B"))
                        actionDescr = "Начало вычисления: PUSH 1";
                    else if (production.equals("B -> a B B"))
                        actionDescr = "Операция дублирования вершины стека (DUP)";
                    else if (production.equals("B -> b"))
                        actionDescr = "Загрузка константы 2 (PUSH 2)";
                    else
                        actionDescr = "ε-правило (ничего не генерируем)";

                    semanticAction.execute(production, actionDescr);
                } else {
                    System.out.println("Ошибка: нет правила для " + top + " на символе " + lookahead);
                    return false;
                }
            }
            // Случай 3: ошибка – неожиданный терминал
            else {
                System.out.println("Ошибка: неожиданный терминал " + top);
                return false;
            }
        }

        accept = (idx == tokens.size());
        if (accept) {
            System.out.println("\n Строка ПРИНЯТА.");
            System.out.println("Семантический результат:\n" + semanticAction.getResult());
        } else {
            System.out.println("\n Строка ОТВЕРГНУТА.");
        }
        return accept;
    }

    /**
     * Генерирует случайную строку, принадлежащую языку грамматики.
     *
     * @param rand генератор случайных чисел
     * @return строка из a и b
     */
    public String generate(Random rand) {
        return generateHelper(startSymbol, rand);
    }

    private String generateHelper(String sym, Random rand) {
        if (!isNonterminal(sym)) return sym; // терминал
        List<List<String>> rules = productions.get(sym);
        List<String> chosen = rules.get(rand.nextInt(rules.size()));
        StringBuilder sb = new StringBuilder();
        for (String s : chosen) {
            sb.append(generateHelper(s, rand));
        }
        return sb.toString();
    }

    /**
     * Выводит на экран FIRST и FOLLOW множества.
     */
    public void printFirstAndFollow() {
        System.out.println("\n  Множества FIRST");
        for (String nt : NONTERMINALS) {
            System.out.println("FIRST(" + nt + ") = " + firstSets.get(nt));
        }
        System.out.println("\n  Множества FOLLOW");
        for (String nt : NONTERMINALS) {
            System.out.println("FOLLOW(" + nt + ") = " + followSets.get(nt));
        }
    }

    /**
     * Выводит LL(1) таблицу разбора.
     */
    public void printParsingTable() {
        System.out.println("\n  LL(1) таблица разбора");
        System.out.print("НТ\t");
        for (String t : TERMINALS) {
            System.out.print(t + "\t");
        }
        System.out.println();
        for (String nt : NONTERMINALS) {
            System.out.print(nt + "\t");
            Map<String, String> row = parsingTable.get(nt);
            for (String t : TERMINALS) {
                String entry = row.getOrDefault(t, "-");
                System.out.print(entry + "\t");
            }
            System.out.println();
        }
    }
}