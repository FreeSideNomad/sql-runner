# F008-S002: Implement Step 1 - Parameter Form

## User Story

**As a** user
**I want** to enter parameters in step 1
**So that** I can define what records will be updated

## Acceptance Criteria

- [ ] Given step 1, then parameter form displayed
- [ ] Given parameters, then same form component as SELECT
- [ ] Given required parameters, then validation enforced
- [ ] Given Next button, then validates and saves to session
- [ ] Given valid parameters, then proceed to step 2

## Technical Notes

### Files to Create
- `src/main/resources/templates/queries/update-steps.html` (fragment: step1)

### Controller
```java
@Controller
@RequestMapping("/queries/{queryId}/update")
@RequiredArgsConstructor
public class UpdateWorkflowController {
    private final UpdateWizardState wizardState;
    private final QueryService queryService;

    @GetMapping
    public String startWizard(@PathVariable String queryId, Model model) {
        wizardState.reset();
        wizardState.setQueryId(queryId);
        wizardState.setCurrentStep(1);

        Query query = queryService.getQueryById(queryId);
        QueryConfig config = queryService.getCurrentConfig(queryId);

        model.addAttribute("query", query);
        model.addAttribute("parameters", config.getParameters());
        model.addAttribute("currentStep", 1);

        return "queries/update-wizard";
    }

    @PostMapping("/step1")
    public String submitStep1(@PathVariable String queryId,
                             @RequestParam Map<String, String> params,
                             RedirectAttributes redirectAttributes) {
        // Validate parameters
        QueryConfig config = queryService.getCurrentConfig(queryId);
        List<String> errors = validateParameters(params, config.getParameters());

        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:/queries/" + queryId + "/update";
        }

        wizardState.setParameters(params);
        wizardState.setCurrentStep(2);

        return "redirect:/queries/" + queryId + "/update/preview";
    }
}
```

### Step 1 Fragment
```html
<div th:fragment="step1">
    <h2 class="text-xl font-semibold mb-4">Step 1: Enter Parameters</h2>
    <p class="text-gray-600 mb-6">
        Define the criteria for records to be updated.
    </p>

    <form th:action="@{/queries/{id}/update/step1(id=${query.id})}" method="post">
        <div th:each="param : ${parameters}">
            <div th:replace="~{fragments/parameter-form :: parameter-input(${param})}"></div>
        </div>

        <!-- Errors -->
        <div th:if="${errors}" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <ul class="list-disc list-inside text-red-700">
                <li th:each="error : ${errors}" th:text="${error}">Error message</li>
            </ul>
        </div>

        <div class="flex justify-end">
            <button type="submit" class="px-6 py-2 bg-rbc-blue text-white rounded-lg">
                Preview Affected Records →
            </button>
        </div>
    </form>
</div>
```

## Test Plan

- [ ] Integration test: Parameters submitted and stored
- [ ] Integration test: Validation errors displayed
- [ ] Integration test: Proceeds to step 2 on valid input

## Parent Feature

Relates to F008-update-workflow
