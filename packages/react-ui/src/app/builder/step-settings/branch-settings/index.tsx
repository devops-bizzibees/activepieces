import { t } from 'i18next';
import React from 'react';
import { useFormContext, useFieldArray } from 'react-hook-form';

import { BranchConditionGroup } from '@/app/builder/step-settings/branch-settings/branch-condition-group';
import { emptyCondition } from '@activepieces/shared';

type BranchSettingsProps = {
  readonly: boolean;
  fieldName?: 'settings.conditions' | `settings.branches[${number}].conditions`;
};

const BranchSettings = React.memo(
  ({ readonly, fieldName = 'settings.conditions' }: BranchSettingsProps) => {
    const form = useFormContext();
    const { fields, append, remove, update } = useFieldArray({
      control: form.control,
      name: fieldName,
    });

    const handleDelete = (groupIndex: number, conditionIndex: number) => {
      const conditions = form.getValues(fieldName);
      const newConditionsGroup = [...conditions[groupIndex]];
      const isSingleGroup = conditions.length === 1;
      const isSingleConditionInGroup = newConditionsGroup.length === 1;

      if (isSingleGroup && isSingleConditionInGroup) {
        update(groupIndex, [emptyCondition]);
      } else if (isSingleConditionInGroup) {
        remove(groupIndex);
      } else {
        newConditionsGroup.splice(conditionIndex, 1);
        update(groupIndex, newConditionsGroup);
      }
    };

    const handleAnd = (groupIndex: number) => {
      const conditions = form.getValues(fieldName);
      conditions[groupIndex] = [...conditions[groupIndex], emptyCondition];
      update(groupIndex, conditions[groupIndex]);
    };

    const handleOr = () => {
      append([[emptyCondition]]);
    };

    return (
      <div className="flex flex-col gap-4" onSubmit={(e) => e.preventDefault()}>
        <div className="text-md ">
          {fieldName === 'settings.conditions'
            ? t('Continue If')
            : t('Execute If')}
        </div>
        {fields.map((fieldGroup, groupIndex) => (
          <BranchConditionGroup
            key={fieldGroup.id}
            readonly={readonly}
            fieldName={fieldName}
            numberOfGroups={fields.length}
            groupIndex={groupIndex}
            onAnd={() => handleAnd(groupIndex)}
            onOr={handleOr}
            handleDelete={(conditionIndex: number) =>
              handleDelete(groupIndex, conditionIndex)
            }
          />
        ))}
      </div>
    );
  },
);

BranchSettings.displayName = 'BranchSettings';
export { BranchSettings };
