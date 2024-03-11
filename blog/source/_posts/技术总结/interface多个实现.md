---
title: interface多个实现
date: 2024-03-11 18:17:22
lang: zh-cn
tags: 
---

# interface多个实现

根据配置使用不同接口实现类

接口：

```java
public interface AIEngine {
    String question(String question);
}
```

实现一：

```java
@Slf4j
@Component
public class AzureComponent implements AIEngine {
	@Override
    public String question(String question) {
        return "AzureComponent question";
    }
}
```

实现二：

```java
@Slf4j
@Component
public class ChatBotComponent implements AIEngine {
	@Override
    public String question(String question) {
        return "ChatBotComponent question";
    }
}
```

根据配置使用对应实现类：

```java
@Component
public class AIEngineInstanse {
    private static AIEngine INSTANCE;

    /**
     * 根据ai.Engine配置，使用不同的实现类
     * @param engine
     * @param azureComponent
     * @param chatBotComponent
     */
    @Autowired
    public AIEngineInstanse(@Value("${ai.Engine}") final String engine, AzureComponent azureComponent, ChatBotComponent chatBotComponent) {
        if (engine.equals("azure")) {
            INSTANCE = azureComponent;
        } else if (engine.equals("chatBot")) {
            INSTANCE = chatBotComponent;
        }
    }

    public static AIEngine getINSTANCE() {
        return INSTANCE;
    }

}
```

上层调用：

```
botAnswer = AIEngineInstanse.getINSTANCE().question(question);
```

‍
