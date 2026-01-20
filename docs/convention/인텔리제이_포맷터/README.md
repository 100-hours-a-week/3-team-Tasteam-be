이 디렉터리에는 IDE(인텔리제이/이클립스)에서 가져와 쓰는 스타일 파일을 두곤 했습니다.

현재 프로젝트에서 실제 적용(Gradle/CI/Husky)되는 포맷터는 Eclipse 설정(`naver-eclipse-formatter.xml`)이며, 아래처럼 정리했습니다.
 - `config/formatter/naver-eclipse-formatter.xml` (Spotless formatter)
 - `config/checkstyle/naver-checkstyle-rules.xml` (Checkstyle rules)
참고로 `config/formatter/intellij-java-wooteco-style.xml`은 IntelliJ IDE 환경에서 사용하는 코드 스타일을 문서로 남겨둔 것이며 Spotless/Eclipse 포맷터는 이 파일을 그대로 활용하지 않습니다.

우테코 스타일 파일의 핵심 포맷팅 특징을 참고용으로 정리하면 다음과 같습니다.
 - 여는/계속 줄 들여쓰기: 들여쓰기 4칸, 이어지는 줄은 `CONTINUATION_INDENT_SIZE=8`으로 구분합니다.
 - 파라미터/호출 줄바꿈: `METHOD_PARAMETERS_WRAP=1`, `CALL_PARAMETERS_WRAP=1`로 어노테이션이나 파라미터가 긴 경우 각 줄을 분리하고
   `ALIGN_MULTILINE_PARAMETERS`/`ALIGN_MULTILINE_FOR`는 비활성화하여 들여쓰기로만 구분하게 합니다.
 - 우선 순위: 기타 줄 바꿈(`ARRAY_INITIALIZER_WRAP`, `BINARY_OPERATION_WRAP`, `IF_BRACE_FORCE` 등)은 모두 “한 줄 이상” 기준(`value="1"`/`"3"`)으로 설정돼 있어 항상 블록 스타일을 유지합니다.
 - 참고: IntelliJ 설정과 Eclipse formatter 사이에는 세세한 들여쓰기/정렬 차이가 있기 때문에, Spotless용 Eclipse 파일은 이 참고 자료를 바탕으로 줄바꿈/들여쓰기 깊이를 조정하고 있습니다.
