import type { Locale } from '../../../../shared/i18n/i18n'

const messages: Record<
  Locale,
  {
    match: string
    limit: string
    partial: string
    noUse: string
    busy: string
    rejected: string
    uncertain: string
    invalid: string
  }
> = {
  en: {
    busy: 'Too many requests. Please wait briefly and try again.',
    match: 'Pattern matched after {n} Chaos uses.',
    limit: 'Reached the limit after {n} Chaos uses. Final result applied.',
    partial:
      'Stopped after {n} confirmed Chaos uses. Final result applied; the next use could not complete.',
    noUse: 'No Chaos use completed. No history step was added.',
    rejected:
      'The repeat request was not accepted. Check the pattern and item.',
    uncertain:
      'Could not confirm the repeat result. No estimated count was recorded. The request will not retry automatically.',
    invalid: 'Could not verify the repeat result. No history step was added.',
  },
  ko: {
    busy: '요청이 많습니다. 잠시 후 다시 시도해 주세요.',
    match: '카오스 {n}회 사용 후 패턴이 일치했습니다.',
    limit: '카오스 {n}회로 한도에 도달했습니다. 마지막 결과를 반영했습니다.',
    partial:
      '확인된 카오스 {n}회 후 중단됐습니다. 마지막 결과를 반영했으며 다음 사용은 완료되지 못했습니다.',
    noUse: '완료된 카오스 사용이 없어 기보를 추가하지 않았습니다.',
    rejected: '반복 요청이 거부됐습니다. 패턴과 아이템을 확인하세요.',
    uncertain:
      '반복 결과를 확인하지 못했습니다. 추정 횟수는 기록하지 않았으며 자동으로 재시도하지 않습니다.',
    invalid: '반복 결과를 검증하지 못해 기보를 추가하지 않았습니다.',
  },
  'zh-CN': {
    busy: '请求过多，请稍后重试。',
    match: '使用混沌石 {n} 次后匹配图案。',
    limit: '使用混沌石 {n} 次后达到上限，已应用最终结果。',
    partial:
      '确认使用混沌石 {n} 次后停止。已应用最终结果，下一次使用未能完成。',
    noUse: '没有完成混沌石使用，未添加历史步骤。',
    rejected: '重复请求未被接受，请检查图案和物品。',
    uncertain: '无法确认重复结果。未记录估计次数，也不会自动重试。',
    invalid: '无法验证重复结果，未添加历史步骤。',
  },
  'zh-TW': {
    busy: '請求過多，請稍後再試。',
    match: '使用混沌石 {n} 次後符合圖樣。',
    limit: '使用混沌石 {n} 次後達到上限，已套用最終結果。',
    partial:
      '確認使用混沌石 {n} 次後停止。已套用最終結果，下一次使用未能完成。',
    noUse: '沒有完成混沌石使用，未新增歷史步驟。',
    rejected: '重複請求未被接受，請檢查圖樣和物品。',
    uncertain: '無法確認重複結果。未記錄估計次數，也不會自動重試。',
    invalid: '無法驗證重複結果，未新增歷史步驟。',
  },
  ja: {
    busy: 'リクエストが多すぎます。しばらく待ってから再試行してください。',
    match: 'カオスを{n}回使用後、パターンが一致しました。',
    limit: 'カオスを{n}回使用して上限に達しました。最終結果を反映しました。',
    partial:
      '確認済みのカオス使用{n}回で停止しました。最終結果を反映し、次の使用は完了できませんでした。',
    noUse: 'カオスの使用は完了せず、履歴は追加していません。',
    rejected:
      '繰り返しリクエストが拒否されました。パターンとアイテムを確認してください。',
    uncertain:
      '繰り返し結果を確認できませんでした。推定回数は記録せず、自動再試行もしません。',
    invalid: '繰り返し結果を検証できず、履歴は追加していません。',
  },
  es: {
    busy: 'Hay demasiadas solicitudes. Espera un momento e inténtalo de nuevo.',
    match: 'El patrón coincidió tras {n} usos de Caos.',
    limit:
      'Se alcanzó el límite tras {n} usos de Caos. Se aplicó el resultado final.',
    partial:
      'Se detuvo tras {n} usos confirmados de Caos. Se aplicó el resultado final; el siguiente uso no pudo completarse.',
    noUse:
      'No se completó ningún uso de Caos. No se añadió un paso al historial.',
    rejected:
      'No se aceptó la solicitud de repetición. Revisa el patrón y el objeto.',
    uncertain:
      'No se pudo confirmar el resultado. No se registró un recuento estimado ni se reintentará automáticamente.',
    invalid:
      'No se pudo verificar el resultado. No se añadió un paso al historial.',
  },

  ru: {
    busy: 'Слишком много запросов. Подождите немного и попробуйте снова.',
    match: 'Шаблон соответствует после использования {n} Chaos.',
    limit:
      'Достигнут предел после использования {n} Chaos. Окончательный результат применен.',
    partial:
      'Остановлено после того, как {n} подтвердил использование Хаоса. Применен окончательный результат; следующее использование не удалось завершить.',
    noUse:
      'Ни одно использование Хаоса не завершено. Ни один шаг истории не был добавлен.',
    rejected: 'Повторный запрос не был принят. Проверьте выкройку и товар.',
    uncertain:
      'Не удалось подтвердить повторный результат. Приблизительного подсчета зафиксировано не было. Запрос не будет повторяться автоматически.',
    invalid:
      'Не удалось проверить повторный результат. Ни один шаг истории не был добавлен.',
  },
  th: {
    busy: 'มีคำขอมากเกินไป โปรดรอสักครู่แล้วลองอีกครั้ง',
    match: 'รูปแบบที่จับคู่หลังจากใช้ {n} Chaos',
    limit: 'ถึงขีดจำกัดแล้วหลังจากที่ {n} Chaos ใช้ ใช้ผลลัพธ์สุดท้ายแล้ว',
    partial:
      'หยุดหลังจาก {n} ยืนยันการใช้ Chaos ใช้ผลลัพธ์สุดท้าย การใช้งานครั้งต่อไปไม่เสร็จสมบูรณ์',
    noUse: 'ไม่มีการใช้ Chaos เสร็จสิ้น ไม่มีการเพิ่มขั้นตอนประวัติ',
    rejected: 'คำขอซ้ำไม่ได้รับการยอมรับ ตรวจสอบแบบและรายการ',
    uncertain:
      'ไม่สามารถยืนยันผลการทำซ้ำได้ ไม่มีการบันทึกการนับโดยประมาณ คำขอจะไม่ลองอีกครั้งโดยอัตโนมัติ',
    invalid: 'ไม่สามารถตรวจสอบผลการทำซ้ำได้ ไม่มีการเพิ่มขั้นตอนประวัติ',
  },
}

export function chaosRepeatResultMessage(
  locale: Locale,
  kind: keyof (typeof messages)['en'],
  uses?: number,
) {
  return messages[locale][kind].replace(
    '{n}',
    uses === undefined ? '' : new Intl.NumberFormat(locale).format(uses),
  )
}
