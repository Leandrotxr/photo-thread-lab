# Photo Thread Lab

Laboratorio de Sistemas Operacionais: blur de fotos com threads em Java.

O app mostra os tres regimes do trabalho:

1. Poucas threads — CPU ociosa, tempo alto
2. N perto dos nucleos — melhor ponto
3. Excesso de threads — oversubscription, tempo volta a piorar

## Requisitos

- JDK 17 (`java -version`)
- O Maven vem no projeto (`mvnw.cmd`)

## Rodar

No PowerShell, dentro desta pasta:

```powershell
.\mvnw.cmd javafx:run
```

## Como usar

1. Clique em **Gerar imagem de teste** (1600x900) ou **Abrir foto...**
2. Deixe a estrategia em **Pool de N threads**
3. Deixe **Colorir tiles por thread** ligado e o **atraso visual** em ~50 ms
4. Clique em **Renderizar** — os blocos nascem no fundo preto, cada cor e uma thread
5. Compare 1, 8 e 64 threads: o preenchimento fica visivelmente mais paralelo
6. **Benchmark** desliga o atraso para medir o tempo real

Tile **128** deixa os blocos grandes e faceis de ver. Atraso **0** se quiser so desempenho.

Compare tambem:

- **Uma thread por tile** — pior: cria/destroi centenas de threads
- **Faixas estaticas** — cada thread pega uma faixa horizontal, sem fila

**Raio do blur** maior = carga maior (melhor para o grafico).

## Comandos

```powershell
.\mvnw.cmd compile
.\mvnw.cmd javafx:run
```
