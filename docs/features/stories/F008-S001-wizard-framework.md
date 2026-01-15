# F008-S001: Create Update Wizard UI Framework (5 Steps)

## User Story

**As a** developer
**I want** a wizard framework for the UPDATE workflow
**So that** the 5-step process is clearly guided

## Acceptance Criteria

- [ ] Given UPDATE query, then wizard UI displayed
- [ ] Given wizard, then 5 steps visible: Parameters, Preview, Confirm, Execute, Result
- [ ] Given current step, then highlighted in progress bar
- [ ] Given step navigation, then forward/back buttons available
- [ ] Given wizard state, then preserved across steps
- [ ] Given back button, then returns to previous step

## Technical Notes

### Files to Create
- `src/main/resources/templates/queries/update-wizard.html`
- `src/main/resources/templates/fragments/wizard-steps.html`
- `src/main/java/com/ivamare/controller/UpdateWorkflowController.java`
- `src/main/java/com/ivamare/dto/UpdateWizardState.java`

### Wizard State DTO (Session Scoped)
```java
@Data
@SessionScope
@Component
public class UpdateWizardState {
    private String queryId;
    private int currentStep = 1;
    private Map<String, String> parameters = new HashMap<>();
    private List<Map<String, Object>> previewData;
    private int affectedRowCount;
    private String backupRecordId;
    private String executionLogId;
}
```

### Step Progress Bar
```html
<div th:fragment="wizard-steps" class="mb-8">
    <div class="flex items-center justify-between">
        <div th:each="step, iter : ${#numbers.sequence(1, 5)}"
             th:class="${step <= currentStep ? 'text-rbc-blue' : 'text-gray-400'}"
             class="flex items-center">
            <div th:class="${step < currentStep ? 'bg-rbc-blue text-white' :
                            step == currentStep ? 'border-2 border-rbc-blue text-rbc-blue' :
                            'border-2 border-gray-300 text-gray-400'}"
                 class="w-10 h-10 rounded-full flex items-center justify-center font-bold">
                <span th:if="${step < currentStep}">✓</span>
                <span th:unless="${step < currentStep}" th:text="${step}">1</span>
            </div>
            <span th:text="${stepNames[step - 1]}" class="ml-2 hidden md:inline">Step Name</span>
            <div th:if="${!iter.last}" class="w-16 h-0.5 mx-4"
                 th:class="${step < currentStep ? 'bg-rbc-blue' : 'bg-gray-300'}"></div>
        </div>
    </div>
</div>
```

### Step Names
```java
@ModelAttribute("stepNames")
public List<String> stepNames() {
    return List.of("Parameters", "Preview", "Confirm", "Execute", "Result");
}
```

### Wizard Container
```html
<div class="max-w-4xl mx-auto">
    <div th:replace="~{fragments/wizard-steps :: wizard-steps}"></div>

    <!-- Step Content -->
    <div class="bg-white rounded-lg shadow p-6">
        <div th:if="${currentStep == 1}" th:replace="~{queries/update-steps :: step1}"></div>
        <div th:if="${currentStep == 2}" th:replace="~{queries/update-steps :: step2}"></div>
        <div th:if="${currentStep == 3}" th:replace="~{queries/update-steps :: step3}"></div>
        <div th:if="${currentStep == 4}" th:replace="~{queries/update-steps :: step4}"></div>
        <div th:if="${currentStep == 5}" th:replace="~{queries/update-steps :: step5}"></div>
    </div>

    <!-- Navigation -->
    <div class="flex justify-between mt-6">
        <button th:if="${currentStep > 1 && currentStep < 5}"
                onclick="previousStep()" class="px-6 py-2 border rounded-lg">
            ← Back
        </button>
        <div th:if="${currentStep == 1}"></div>
        <button th:if="${currentStep < 4}"
                onclick="nextStep()" class="px-6 py-2 bg-rbc-blue text-white rounded-lg">
            Next →
        </button>
    </div>
</div>
```

## Test Plan

- [ ] Visual test: Progress bar shows correct step
- [ ] Integration test: State preserved across steps
- [ ] Integration test: Back navigation works

## Parent Feature

Relates to F008-update-workflow
