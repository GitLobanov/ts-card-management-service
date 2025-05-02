package by.lobanov.cardmanagementservice.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Создано как замена отправки Page напрямую
 * @param <T>
 */
@Data
@Schema(description = "Стандартизированный ответ для пагинированных списков")
public class PagedResponse<T> {

    @Schema(description = "Содержимое текущей страницы")
    private List<T> content;

    @Schema(description = "Номер текущей страницы (начиная с 0)", example = "0")
    private int pageNumber;

    @Schema(description = "Размер страницы", example = "10")
    private int pageSize;

    @Schema(description = "Общее количество элементов во всех страницах", example = "100")
    private long totalElements;

    @Schema(description = "Общее количество страниц", example = "10")
    private int totalPages;

    @Schema(description = "Является ли текущая страница первой", example = "true")
    private boolean first;

    @Schema(description = "Является ли текущая страница последней", example = "false")
    private boolean last;

    // Статический фабричный метод для удобного создания из Page
    public static <T> PagedResponse<T> fromPage(Page<T> page) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setContent(page.getContent());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        return response;
    }
}
