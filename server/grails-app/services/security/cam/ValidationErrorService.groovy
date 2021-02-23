package security.cam

import grails.gorm.transactions.Transactional
import grails.validation.ValidationErrors

@Transactional
class ValidationErrorService {

    Map commandErrors(ValidationErrors validationErrors, String serviceName) {
        def errorMap = [:]

        def fieldErrors = validationErrors.getAllErrors()

        fieldErrors.each {
            String errorMsg = ''

            String fieldName = it.field
            // obtain any previous error message
            if (errorMap[fieldName]) {
                errorMsg += errorMap[fieldName]
            }

            def errorCode = it.getCode()

            if (errorCode.equals('null')) {
                errorMsg += fieldName + " must not be null (empty). "
            }
            else if (errorCode.equals('nullable')) {
                errorMsg += fieldName + " must not be null (empty). "
            }
            else if (errorCode.equals('blank')) {
                errorMsg += fieldName + " must not be blank (empty). "
            }
            else if (errorCode.equals('size.toobig')) {
                errorMsg += fieldName + " is too long. Reduce the length. "
            }
            else if (errorCode.equals('matches.invalid')) {
                errorMsg += fieldName + " is not in the correct format. "
            }
            else if (errorCode.equals('match.check.cannot.be.made')) {
                errorMsg += fieldName + " format cannot be checked because of a problem with the matching pattern. Please inform your supervisor. "
            }
            else if (errorCode.equals('min.notmet')) {
                errorMsg += fieldName + " is too small. Increase this value. "
            }
            else if (errorCode.equals('max.exceeded')) {
                errorMsg += fieldName + " is too large. Reduce this value. "
            }
            else if (errorCode.equals('list.element.out.of.range')) {
                errorMsg += "One or more of the values in this list is outside the permitted range. "
            }
            else if (errorCode.equals('wrong.state')) {
                errorMsg += " is not in the correct state for this action. "
            }
            else if (errorCode.equals('duplicate')) {
                errorMsg += 'A record with that '+fieldName + ' already exists in the fleet. Please choose another '+fieldName+'. '
            }
            else
               errorMsg += errorCode

            errorMap[fieldName] = errorMsg
        }
        return errorMap
    }
}
